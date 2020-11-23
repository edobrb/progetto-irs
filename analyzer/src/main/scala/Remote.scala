import java.net._

import utils.Parallel.Parallel
import java.net.ServerSocket
import java.util.concurrent.{BlockingDeque, ExecutorService, Executors, LinkedBlockingDeque}

import model.config.Configuration.JsonFormats._
import com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker
import model.{RobotData, StepInfo}
import model.config.Configuration
import play.api.libs.json.{Json, OFormat}
import utils.RichIterator.RichIterator

import scala.util.{Failure, Success, Try}
import utils.RichSocket._

object Remote extends App {
  def KEEP_ALIVE_MSG = "ka"

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


case class DispatcherServer(server: ServerSocket) {

  def run(args: Array[String]): Unit = {
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

    val work: BlockingDeque[(String, Configuration, Int)] = new LinkedBlockingDeque[(String, Configuration, Int)]()
    val finished: BlockingDeque[(String, Configuration, Int)] = new LinkedBlockingDeque[(String, Configuration, Int)]()
    experiments.foreach(work.addLast)

    do {
      while (!work.isEmpty) {
        val (name, config, i) = work.takeFirst()
        val filename = Args.DATA_FOLDER(args) + "/" + name
        val loaded_output_filename = filename + ".json"
        val socket = server.accept
        executor.execute(() => {
          val (done, time) = utils.Benchmark.time {
            Try(DispatcherClient(socket).execute(name, config)).map(result => utils.File.write(loaded_output_filename, result))
          }
          done match {
            case Success(_) =>
              println(s"Success    $name from ${socket.getRemoteSocketAddress}. (${time.toSeconds}s)")
              finished.addLast((name, config, i))
            case Failure(_) =>
              work.addFirst((name, config, i))
              println(s"Failure    $name from ${socket.getRemoteSocketAddress}. (${time.toSeconds}s)")
          }
        })
      }
      Thread.sleep(100)
    } while (finished.size() != experiments.size)
    println("All experiments are done!")
  }
}

case class DispatcherClient(client: Socket) {
  def execute(name: String, config: Configuration): String = {
    client.writeStr(config.toJson)
    println(s"Dispatched $name to ${client.getRemoteSocketAddress}")
    var result = ""
    do {
      result = client.readStr()
    } while (result == Remote.KEEP_ALIVE_MSG)
    client.close()
    result
  }
}

case class RunnerClient(client: Socket) {
  implicit val siCodec: JsonValueCodec[StepInfo] = JsonCodecMaker.make
  implicit val dataFormat: OFormat[RobotData] = Json.format[RobotData]

  def execute(args: Array[String]): Unit = {
    val config = Configuration.fromJson(client.readStr())
    val name = config.setControllersSeed(None).setSimulationSeed(None).filename
    println(s"Configuration $name received...")
    val ((robotsData, lines), time) = utils.Benchmark.time {
      val out = Experiments.runSimulation(config, visualization = false)(args)
      var lines = 1 //configuration not included
      val data = out.map(Loader.toStepInfo).collect { case Some(info) => info }.zipWithIndex.map({
        case (info, i) if i % 100000 == 0 =>
          lines = lines + 1
          client.writeStr(Remote.KEEP_ALIVE_MSG)
          info
        case (info, _) =>
          lines = lines + 1
          info
      })
      (Loader.extractTests2(data, config), lines)
    }
    if (lines == config.expectedLines) {
      val result = Json.prettyPrint(Json.toJson(robotsData))
      client.writeStr(result)
      println(s"Configuration $name done. (${time.toSeconds}s)")
    } else {
      println(s"Configuration $name error. (${time.toSeconds}s)")
    }
    client.close()
  }
}