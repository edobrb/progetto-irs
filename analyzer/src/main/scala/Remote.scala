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
    (0 until Int.MaxValue).parForeach(Args.PARALLELISM_DEGREE(args), {
      _ =>
        Try(RunnerClient(new Socket(address, port)).execute(args)) match {
          case Failure(exception) =>
            //println(exception)
            Thread.sleep(10000)
          case Success(_) =>
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
    val experiments = Settings.experiments(args).sortBy(_._3)
    val executor: ExecutorService = Executors.newCachedThreadPool()
    println(s"Server started, ${experiments.size} experiments to dispatch")
    experiments.iterator.zip(Iterator.continually(server.accept)).foreach {
      case ((name, config, _), socket) =>
        val filename = Args.DATA_FOLDER(args) + "/" + name
        val loaded_output_filename = filename + ".json"
        executor.execute(() => {
          val (done, time) = utils.Benchmark.time {
            val result = DispatcherClient(socket).execute(config)
            utils.File.write(loaded_output_filename, result)
          }
          done match {
            case Success(_) => println(s"$name success in ${time.toSeconds}")
            case Failure(_) => println(s"$name failure in ${time.toSeconds}")
          }
        })
    }
  }
}

case class DispatcherClient(client: Socket) extends Messenger {
  override def in: InputStream = client.getInputStream

  override def out: OutputStream = client.getOutputStream

  def execute(config: Configuration): String = {
    writeStr(config.toJson)
    val result = readStr
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
    println("New configuration received... " + config.setControllersSeed(None).setSimulationSeed(None).filename)
    val out = Experiments.runSimulation(config, visualization = false)(args)
    val data = out.map(Loader.toStepInfo).collect { case Some(info) => info }
    val robotsData = Loader.extractTests2(data, config)
    val result = Json.prettyPrint(Json.toJson(robotsData))
    writeStr(result)
    println("Result pushed, closing...")
    client.close()
  }
}