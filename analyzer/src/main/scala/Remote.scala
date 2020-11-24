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
  def VERSION = "0.0.1"
  def KEEP_ALIVE_MS = 10000

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
            case Success(version) if version == VERSION =>
            case Success(version) if version != VERSION =>
              println("Wrong remote version. Exiting...")
              System.exit(0)
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
    val experiments = Settings.experiments(args).sortBy(_._3).filter {
      case (name, _, i) =>
        val filename = Args.DATA_FOLDER(args) + "/" + name
        val loaded_output_filename = filename + ".json"
        val exists = utils.File.exists(loaded_output_filename)
        if (exists) println(s"Skipping $loaded_output_filename")
        !exists
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
        val socket = server.accept
        executor.execute(() => {
          val (done, time) = utils.Benchmark.time {
            Try(DispatcherClient(socket).execute(name, config, executor)).map(result => utils.File.write(loaded_output_filename, result))
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
  def execute(name: String, config: Configuration, executor: ExecutorService): String = {
    client.setSoTimeout(Remote.KEEP_ALIVE_MS * 2)
    client.writeStr(Remote.VERSION)
    client.writeStr(config.toJson)
    println(s"Dispatched $name to ${client.getRemoteSocketAddress}")

    var result = ""
    keepAlive()
    def keepAlive(): Unit = executor.execute(() => {
      Thread.sleep(Remote.KEEP_ALIVE_MS)
      Try(client.writeStr(Remote.KEEP_ALIVE_MSG)) match {
        case Failure(_) => client.close()
        case Success(_) => if(result == Remote.KEEP_ALIVE_MSG || result.isEmpty) keepAlive()
      }
    })

    do {
      result = client.readStr()
    } while (result == Remote.KEEP_ALIVE_MSG)
    client.writeStr("end")
    client.close()
    result
  }
}

case class RunnerClient(client: Socket) {
  implicit val siCodec: JsonValueCodec[StepInfo] = JsonCodecMaker.make
  implicit val dataFormat: OFormat[RobotData] = Json.format[RobotData]

  def execute(args: Array[String]): String = {
    client.setSoTimeout(Remote.KEEP_ALIVE_MS * 2)
    val version = client.readStr()
    if(version == Remote.VERSION) {
      val config = Configuration.fromJson(client.readStr())
      val name = config.setControllersSeed(None).setSimulationSeed(None).filename
      println(s"Configuration $name received...")
      val executor: ExecutorService = Executors.newCachedThreadPool()
      var brokenConnection = false
      readKeepAlive()
      def readKeepAlive(): Unit = executor.execute(() => {
        val res = Try(client.readStr()).toOption
        brokenConnection = brokenConnection || res.isEmpty
        if(res.isDefined && !res.contains("end")) readKeepAlive()
      })

      val ((robotsData, lines), time) = utils.Benchmark.time {
        val out = Experiments.runSimulation(config, visualization = false)(args)
        var lines = 1 //configuration not included
        var lastKeepAlive = System.currentTimeMillis()
        val data = out.map(Loader.toStepInfo).collect { case Some(info) => info }.map({
          info =>
            lines = lines + 1
            if(brokenConnection) throw new Exception("Keep alive timeout")
            if ((System.currentTimeMillis() - lastKeepAlive) > Remote.KEEP_ALIVE_MS) {
              client.writeStr(Remote.KEEP_ALIVE_MSG)
              lastKeepAlive = System.currentTimeMillis()
            }
            info
        })
        (Loader.extractTests2(data, config), lines)
      }

      executor.shutdown()
      if (lines == config.expectedLines) {
        val result = Json.prettyPrint(Json.toJson(robotsData))
        client.writeStr(result)
        println(s"Configuration $name done. (${time.toSeconds}s)")
      } else {
        println(s"Configuration $name error. (${time.toSeconds}s)")
      }
    }
    client.close()
    version
  }
}