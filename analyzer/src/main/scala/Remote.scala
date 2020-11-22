import java.net._
import java.io._
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

import utils.Parallel.Parallel
import java.net.ServerSocket
import java.util.concurrent.{ExecutorService, Executors}

import model.config.Configuration.JsonFormats._

import com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker
import model.{RobotData, StepInfo}
import model.config.Configuration
import play.api.libs.json.{Json, OFormat}

import scala.util.{Failure, Success, Try}

object Remote extends App {

  val client = utils.Arguments.boolOrDefault("client", default = false)(args)
  val server = utils.Arguments.boolOrDefault("server", default = false)(args)
  val port = utils.Arguments.argOrException("port", Some.apply)(args).toInt
  if (client) {
    val address = utils.Arguments.argOrException("address", Some.apply)(args)
    (0 until Args.PARALLELISM_DEGREE(args)).parForeach(Args.PARALLELISM_DEGREE(args), {
      _ =>
        while (true) {
          Try(RunnerClient(new Socket(address, port)).execute(args)) match {
            case Failure(exception) =>
              println(s"${exception.getMessage}. Retry in 10 seconds")
              Thread.sleep(10000)
            case Success(_) =>
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


trait Messenger {
  def in: InputStream

  def out: OutputStream

  def writeStr(s: String): Unit = {
    val bytes = s.getBytes("utf8")
    write(bytes)
  }

  def readStr: String = {
    val bytes = read()
    new String(bytes, StandardCharsets.UTF_8)
  }

  def write(message: Array[Byte]): Unit = {
    out.write(ByteBuffer.allocate(4).putInt(message.length).array())
    out.write(message)
    out.flush()
  }

  def read(): Array[Byte] = {
    val byteLength = in.readNBytes(4)
    val length = ByteBuffer.wrap(byteLength).getInt
    in.readNBytes(length)
  }

}

case class DispatcherServer(server: ServerSocket) {

  def run(args: Array[String]): Unit = {
    var failed = false
    val experiments = Settings.experiments(args).filter {
      case (name, _, i) =>
        val filename = Args.DATA_FOLDER(args) + "/" + name
        val loaded_output_filename = filename + ".json"
        val exists = utils.File.exists(loaded_output_filename)
        if (exists) println(s"Skipping $loaded_output_filename")
        !exists
    }.sortBy(_._3)
    val executor: ExecutorService = Executors.newCachedThreadPool()
    println(s"Server started, ${experiments.size} experiments to dispatch")
    experiments.iterator.zip(Iterator.continually(server.accept)).foreach {
      case ((name, config, _), socket) =>
        val filename = Args.DATA_FOLDER(args) + "/" + name
        val loaded_output_filename = filename + ".json"

        executor.execute(() => {
          val (done, time) = utils.Benchmark.time {
            Try(DispatcherClient(socket).execute(config)).map(result => utils.File.write(loaded_output_filename, result))
          }
          done match {
            case Success(_) => println(s"$name success in ${time.toSeconds}s")
            case Failure(_) => failed = true; println(s"$name failure in ${time.toSeconds}s")
          }
        })
    }
    if(failed) run(args)
  }
}

case class DispatcherClient(client: Socket) extends Messenger {
  override def in: InputStream = client.getInputStream

  override def out: OutputStream = client.getOutputStream

  def execute(config: Configuration): String = {
    writeStr(config.toJson)
    var result = ""
    do {
      result = readStr
    } while (result == "keep alive")
    client.close()
    result
  }
}

case class RunnerClient(client: Socket) extends Messenger {
  implicit val siCodec: JsonValueCodec[StepInfo] = JsonCodecMaker.make


  implicit val dataFormat: OFormat[RobotData] = Json.format[RobotData]

  override def in: InputStream = client.getInputStream

  override def out: OutputStream = client.getOutputStream

  def execute(args: Array[String]): Unit = {
    val config = Configuration.fromJson(readStr)
    val name = config.setControllersSeed(None).setSimulationSeed(None).filename
    println(s"Configuration $name received...")
    val out = Experiments.runSimulation(config, visualization = false)(args)
    var lines = 1 //configuration not included
    val data = out.map(Loader.toStepInfo).collect { case Some(info) => info }.zipWithIndex.map({
      case (info, i) if i % 100000 == 0 =>
        lines = lines + 1
        writeStr("keep alive")
        info
      case (info, _) =>
        lines = lines + 1
        info
    })
    val robotsData = Loader.extractTests2(data, config)
    if(lines == config.expectedLines) {
      val result = Json.prettyPrint(Json.toJson(robotsData))
      writeStr(result)
      println(s"Configuration $name done.")
    } else {
      println(s"Configuration $name error.")
    }
    client.close()
  }
}