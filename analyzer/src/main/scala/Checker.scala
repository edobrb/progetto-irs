import model.config.Config
import play.api.libs.json.Json
import utils.Parallel.Parallel

import scala.util.{Failure, Success, Try}
import com.github.plokhotnyuk.jsoniter_scala.macros._
import com.github.plokhotnyuk.jsoniter_scala.core._
import model.StepInfo

object Checker extends App {

  implicit val arguments: Array[String] = args

  implicit val codec: JsonValueCodec[StepInfo] = JsonCodecMaker.make

  val asd = utils.Benchmark.time(utils.File.readGzippedLinesAndMap("C:\\Users\\Edo\\Documents\\data\\f38887ad4f2a14b18cdb28b2c1e87915944a49e14ba7b2804f3c24cafd7f4123-72.gzip") {
    it => it.drop(1).map(v => readFromString[StepInfo](v+"asd")).to(Seq)
  })
  println(asd._1.get.size)
  println(asd._2.toSeconds)
  /*Loader.INPUT_FILENAMES.parForeach(threads = Settings.PARALLELISM_DEGREE, file => {
    utils.File.readGzippedLines(file) match {
      case Failure(exception) => //println(s"$file [FAILURE] Error: ${exception.getMessage}") //file not exists probably
      case Success((content, source)) =>
        Try {
          val config: Config = Config.fromJson(content.next())
          (content.length + 1, config)
        } match {
          case Failure(exception) => println(s"$file [FAILURE] Error: ${exception.getMessage}")
          case Success((lines, config)) if lines != config.expectedLines => println(s"$lines/${config.expectedLines} [FAILURE]")
          case Success((lines, config)) => println(s"$file [SUCCESS] $lines/${config.expectedLines} ")
        }
        source.close()
    }
  })*/
}
