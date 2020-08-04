import analysis.Functions
import model.Types.RobotId
import model.config.Config
import model.config.Config.JsonFormats._
import model.{BooleanNetwork, TestRun}
import play.api.libs.json.{Json, OFormat}
import utils.Benchmark
import utils.RichParIterable._

import scala.collection.parallel.CollectionConverters._
import scala.concurrent.duration.FiniteDuration
import scala.util.{Failure, Success}

object Loader extends App {

  def filenames: Iterable[String] = Experiments.experiments.keys.map(Experiments.DATA_FOLDER + "/" + _)

  case class Data(filename: String, config: Config, robot_id: String, fitness_curve: Seq[Double], fitness_values: Seq[Double], bestBn: BooleanNetwork.Schema)

  implicit def dataFormat: OFormat[Data] = Json.format[Data]

  filenames.toList.par.parallelism(2).foreach(filename => {
    println(s"Loading $filename ... ")
    utils.File.readGzippedLines2(filename) {
      content: Iterator[String] =>
        val config: Config = Config.fromJson(content.next())
        val (results: Map[RobotId, Seq[TestRun]], time: FiniteDuration) = Benchmark.time {
          Functions.extractTests(content.map(Functions.toStepInfo).collect { case Some(info) => info }.to(LazyList))
        }
        println(s"done. (${time.toSeconds} s)")
        (config, results)
    } match {
      case Failure(exception) => println(s"$filename throw an exception: ${exception.getMessage}"); Seq()
      case Success((config: Config, result: Map[RobotId, Seq[TestRun]])) =>
        val experimentData = result.map {
          case (robotId, tests) =>
            val fitnessCurve = tests.scanLeft(0.0) {
              case (fitness, test) if test.fitnessValues.last > fitness => test.fitnessValues.last
              case (fitness, _) => fitness
            }.drop(1) //remove the initial 0.0
            Data(filename, config, robotId, fitnessCurve, tests.map(_.fitnessValues.last), tests.maxBy(_.fitnessValues.last).bn)
        }
        System.gc()
        val r = Runtime.getRuntime
        println("Memory usage: " + (r.totalMemory - r.freeMemory) + " of " + r.maxMemory)
        utils.File.write(filename + ".json", Json.prettyPrint(Json.toJson(experimentData)))
    }
  })
}
