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
  Settings.experiments.toList.sortBy(_._3).parForeach(threads = Settings.PARALLELISM_DEGREE, {
    case (experimentName, config, _) =>
      val filename = Settings.DATA_FOLDER + "/" + experimentName
      val output_filename = filename + ".gzip"
      if (!utils.File.exists(output_filename)) {
        Thread.sleep(Random.nextInt(100)) //In order to generate different seed for randon inside argos
        val expectedLines = config.expectedLines
        Benchmark.time {
          println(s"Started experiment $experimentName ...")
          val out = config.toJson +: runSimulation(config, visualization = false).filter(_.headOption.contains('{'))
          if (Settings.argOrDefault("load", v => Try(v.toBoolean).toOption, false)(arguments)) { //Loading now
            val lines = out.to(LazyList)
            (utils.File.writeGzippedLines(output_filename, lines.iterator), Some(lines))
          } else {
            (utils.File.writeGzippedLines(output_filename, out), None)
          }
        } match {
          case ((Success(lines), out), time) if lines == expectedLines =>
            println(s"Done experiment $experimentName (${time.toSeconds} s, $lines/$expectedLines lines) [SUCCESS]")
            out.foreach { v => //Loading now
              Loader.load(v.iterator, filename + ".json") match {
                case Failure(exception) => println(s"Error while loading ${filename + ".json"}: ${exception.getMessage}")
                case Success(timeLoad) => println(s"Loading of ${filename + ".json"} done in ${timeLoad.toSeconds})")
              }
            }
          case ((Success(lines), _), time) if lines != expectedLines =>
            println(s"Done experiment $experimentName (${time.toSeconds} s, $lines/$expectedLines lines) [FAILURE]")
          case ((Failure(exception), _), time) =>
            println(s"Failed experiment $experimentName (${time.toSeconds} s, error: ${exception.getMessage}) [FAILURE]")
        }
      } else {
        println("Skipping " + experimentName)
      }
  })
}
