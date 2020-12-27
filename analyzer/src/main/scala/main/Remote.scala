package main

import java.io.{ByteArrayOutputStream, PrintWriter}
import java.net.{ServerSocket, _}
import java.util.concurrent.{BlockingDeque, ExecutorService, Executors, LinkedBlockingDeque}
import java.util.zip.GZIPOutputStream

import com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker
import model.config.Configuration
import model.{RobotData, StepInfo}
import play.api.libs.json.{Json, OFormat}
import utils.Parallel.Parallel
import utils.RichIterator.RichIterator
import utils.{Argos, RichSocket}

import scala.concurrent.duration.Duration
import scala.util.{Failure, Success, Try}
import model.config.Configuration.JsonFormats._

object Remote extends App {
  def KEEP_ALIVE_MS = 10000

  val client = utils.Arguments.boolOrDefault("client", default = false)(args)
  val server = utils.Arguments.boolOrDefault("server", default = false)(args)
  val port = utils.Arguments.argOrException("port", Some.apply)(args).toInt
  if (client) {
    val address = utils.Arguments.argOrException("address", Some.apply)(args)
    (0 until Args.PARALLELISM_DEGREE(args)).parForeach(Args.PARALLELISM_DEGREE(args), {
      _ =>
        while (true) {
          val result = for (socket <- Try(new Socket(address, port));
                            _ = socket.setSendBufferSize(50 * 1024 * 1024);
                            result <- RunnerClient(socket).execute(args)) yield result
          result match {
            case Failure(exception) =>
              println(s"${exception.getMessage}. Retry in ${KEEP_ALIVE_MS / 1000} seconds")
              Thread.sleep(KEEP_ALIVE_MS)
            case Success((time, name)) => println(s"Configuration $name done. (${time.toSeconds}s)")
          }
        }
    })
  }
  if (server) {
    val serverSocket = new ServerSocket(port)
    DispatcherServer(serverSocket).run(args)
    serverSocket.close()
  }
}


case class DispatcherServer(server: ServerSocket) {

  def run(args: Array[String]): Unit = {
    val load = Args.LOAD_OUTPUT(args)
    val write = Args.WRITE_OUTPUT(args)
    val experiments = Settings.experiments(args).sortBy(_._3).filter {
      case (name, _, i) =>
        val filename = Args.DATA_FOLDER(args) + "/" + name
        val loaded_output_filename = filename + ".json"
        val output_filename = filename + ".gzip"
        val existsLoad = (!load) || utils.File.exists(loaded_output_filename)
        val existsWrite = (!write) || utils.File.exists(output_filename)
        val existsBoth = existsLoad && existsWrite
        if (existsBoth) println(s"Skipping $loaded_output_filename")
        !existsBoth
    }
    val executor: ExecutorService = Executors.newCachedThreadPool()
    println(s"Server started, ${experiments.size} experiments to dispatch")

    val work: BlockingDeque[(String, Configuration, Int)] = new LinkedBlockingDeque[(String, Configuration, Int)]()
    val finished: BlockingDeque[(String, Configuration, Int)] = new LinkedBlockingDeque[(String, Configuration, Int)]()
    experiments.foreach(work.addLast)

    do {
      while (!work.isEmpty) {
        val (name, config, i) = work.takeFirst()
        val filename = Args.DATA_FOLDER(args) + "/" + name
        val loaded_output_filename = filename + ".json"
        val output_filename = filename + ".gzip"
        val socket = server.accept
        socket.setReceiveBufferSize(50 * 1024 * 1024)
        executor.execute(() => {
          val (done, time) = utils.Benchmark.time {
            DispatcherClient(socket).execute(name, config, write, load)
              .map {
                case (maybeData, maybeJson) =>
                  maybeJson.foreach(json => utils.File.write(loaded_output_filename, json))
                  maybeData.foreach(bytes => utils.File.write(output_filename, bytes))
              }
          }
          done match {
            case Success(_) =>
              println(s"Success    $name from ${socket.getRemoteSocketAddress}. (${time.toSeconds}s)")
              finished.addLast((name, config, i))
            case Failure(ex) =>
              work.addFirst((name, config, i))
              println(s"Failure    $name from ${socket.getRemoteSocketAddress}. (${time.toSeconds}s) ${ex.getMessage}")
          }
        })
      }
      Thread.sleep(100)
    } while (finished.size() != experiments.size)
    println("All experiments are done!")
  }
}

case class DispatcherClient(socket: Socket) {

  def execute(name: String, config: Configuration, write: Boolean, load: Boolean): Try[(Option[Array[Byte]], Option[String])] = {
    val client = RichSocket(socket, Remote.KEEP_ALIVE_MS)
    client.writeStr(config.toJson)
    client.writeStr(write.toString)
    client.writeStr(load.toString)
    println(s"Dispatched $name to ${client.socket.getRemoteSocketAddress}")
    val result = (write, load) match {
      case (true, false) => client.read().map(data => (Some(data), None))
      case (false, true) => client.readStr().map(json => (None, Some(json)))
      case (true, true) => for (data <- client.read(); json <- client.readStr()) yield (Some(data), Some(json))
    }
    client.writeStr("done")
    client.richClose()
    result
  }
}

case class RunnerClient(socket: Socket) {
  implicit val siCodec: JsonValueCodec[StepInfo] = JsonCodecMaker.make
  implicit val dataFormat: OFormat[RobotData] = Json.format[RobotData]

  def execute(args: Array[String]): Try[(Duration, String)] = {
    val client = RichSocket(socket, Remote.KEEP_ALIVE_MS)

    for (config <- client.readStr().map(Configuration.fromJson);
         write <- client.readStr().map(_.toBoolean);
         load <- client.readStr().map(_.toBoolean)) yield {

      val name = config.setControllersSeed(None).setSimulationSeed(None).filename
      println(s"Configuration $name received (load=$load, write=$write) ...")

      val ((raw: Option[Array[Byte]], robotsData: Option[Seq[RobotData]], lines), time) = utils.Benchmark.time {
        val (experimentStdOut, experimentProcess) = Argos.runConfiguredSimulation(Args.WORKING_DIR(args), config, visualization = false)
        var lines = 0
        val output = (config.toJson +: experimentStdOut.filter(_.headOption.contains('{'))).map({ l =>
          lines = lines + 1
          if (socket.isClosed) experimentProcess.destroy()
          l
        })

        def loadRaw(lines: Iterator[String]): Array[Byte] = {
          val output = new ByteArrayOutputStream(20 * 1024 * 1024)
          val gzip = new GZIPOutputStream(output)
          val writer = new PrintWriter(gzip)
          lines.foreach(line => writer.write(line + "\n"))
          writer.flush()
          writer.close()
          output.toByteArray
        }

        def loadStepInfo(lines: Iterator[String]): Iterator[StepInfo] =
          lines.drop(1).map(Loader.toStepInfo).collect { case Some(info) => info }

        (write, load) match {
          case (true, false) =>
            (Some(loadRaw(output)), None, lines)
          case (false, true) =>
            (None, Some(Loader.extractTests3(loadStepInfo(output), config)), lines)
          case (true, true) =>
            val (output1, output2) = output.copy(128)
            val (raw, json) = utils.Parallel.run2(Some(loadRaw(output1)), Some(Loader.extractTests3(loadStepInfo(output2), config)))
            (raw, json, lines)
        }
      }

      Try {
        if (lines == config.expectedLines) {
          raw.foreach { data =>
            client.write(data).recover(ex => throw ex)
          }
          robotsData.foreach { data =>
            client.writeStr(Json.prettyPrint(Json.toJson(data))).recover(ex => throw ex)
          }
          if(!client.readStr().toOption.contains("done")) {
            throw new Exception(s"Experiment done but 'done' was not received")
          }
        } else {
          throw new Exception(s"Wrong lines number")
        }
        client.richClose()
      }.map(_ => (time, name))
    }
  }.flatten
}