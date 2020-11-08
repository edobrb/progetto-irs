import model.config.Config
import play.api.libs.json.Json
import utils.Parallel.Parallel

import scala.util.{Failure, Success, Try}
import com.github.plokhotnyuk.jsoniter_scala.macros._
import com.github.plokhotnyuk.jsoniter_scala.core._
import model.StepInfo

object Checker extends App {

  implicit val arguments: Array[String] = args

  Loader.INPUT_FILENAMES.parForeach(threads = Settings.PARALLELISM_DEGREE, file => {
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
  })
}
