import model.config.Config
import model.config.Config.JsonFormats._
import play.api.libs.json.Json
import utils.Parallel._
import utils.RichIterator._
import utils.{Argos, Benchmark}

import scala.util.{Failure, Random, Success, Try}

object Experiments extends App {

  implicit val arguments: Array[String] = args

  /** Simulation standard output (by lines) */
  def runSimulation(config: Config, visualization: Boolean)(implicit args: Array[String]): Iterator[String] =
    Argos.runConfiguredSimulation(Settings.WORKING_DIR(args), Settings.SIMULATION_FILE(args), config, visualization)

  /** Running experiments */
  println(s"Running ${Settings.experiments.size} experiments...")
  Settings.experiments.toList.sortBy(_._1).parForeach(threads = Settings.PARALLELISM_DEGREE, {
    case (experimentName, config) =>
      val filename = Settings.DATA_FOLDER + "/" + experimentName
      if (!utils.File.exists(filename)) {
        Thread.sleep(Random.nextInt(100)) //In order to generate different seed for randon inside argos
        val expectedLines = config.expectedLines
        Benchmark.time {
          println(s"Started experiment $experimentName ...")
          val out = config.toJson +: runSimulation(config, visualization = false)
          utils.File.writeGzippedLines(filename, out)
        } match {
          case (Success(lines), time) if lines == expectedLines =>
            println(s"Done experiment $experimentName (${time.toSeconds} s, $lines/$expectedLines lines) [SUCCESS]")
          case (Success(lines), time) if lines != expectedLines =>
            println(s"Done experiment $experimentName (${time.toSeconds} s, $lines/$expectedLines lines) [FAILURE]")
          case (Failure(exception), time) =>
            println(s"Failed experiment $experimentName (${time.toSeconds} s, error: ${exception.getMessage}) [FAILURE]")
        }
      } else {
        println("Skipping " + experimentName)
      }
  })
}
