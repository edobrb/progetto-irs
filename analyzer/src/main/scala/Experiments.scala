import model.config.Config
import model.config.Config.JsonFormats._
import utils.Parallel._
import utils.RichIterator._
import utils.{Argos, Benchmark}

import scala.util.Random

object Experiments extends App {

  implicit val arguments: Array[String] = args

  /** Simulation standard output (by lines) */
  def runSimulation(config: Config, visualization: Boolean = false)(implicit args: Array[String]): Iterator[String] =
    Argos.runConfiguredSimulation(Settings.WORKING_DIR(args), Settings.SIMULATION_FILE(args), config, visualization)

  /** Running experiments */
  println(s"Running ${Settings.experiments.size} experiments...")
  Settings.experiments.toList.sortBy(_._1).parForeach(threads = Settings.PARALLELISM_DEGREE, {
    case (experimentName, config) =>
      val filename = Settings.DATA_FOLDER + "/" + experimentName
      if (!utils.File.exists(filename)) {
        Thread.sleep(Random.nextInt(100))
        val expectedLines = config.simulation.experiment_length * config.simulation.ticks_per_seconds * config.simulation.robot_count
        Benchmark.time {
          println(s"Started experiment $experimentName ...")
          val out = config.toJson +: runSimulation(config)
          utils.File.writeGzippedLines(filename, out)
        } match {
          case (lines, time) => println(s"Done experiment $experimentName (${time.toSeconds} s, $lines/$expectedLines lines)")
        }
      } else {
        println("Skipping " + experimentName)
      }
  })
}
