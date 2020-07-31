import analysis.Functions
import model.Types.RobotId
import model.config.Config
import model.config.Config.JsonFormats._
import model.{BooleanNetwork, TestRun}
import play.api.libs.json.{Json, OFormat}
import utils.Benchmark

import scala.concurrent.duration.FiniteDuration
import scala.util.{Failure, Success}

object Loader extends App {

  //val filenames = Experiments.configs.keys.flatMap(f => (1 to 30).map(Experiments.DATA_FOLDER + "/" + f + "-" + _))
  val filenames = Seq("0.1", "0.5", "0.79").flatMap(f => (1 to 30).map(Experiments.DATA_FOLDER + "/" + f + "-" + _))

  case class Data(filename: String, config: Config, robot_id: String, fitness_curve: Seq[Double], bns: Seq[BooleanNetwork.Schema])

  implicit def dataFormat: OFormat[Data] = Json.format[Data]

  filenames.foreach(filename => {
    print(s"Loading $filename ... ")
    utils.File.readGzippedLines2(filename) {
      content: Seq[String] =>
        val config: Config = Config.fromJson(content.head)
        val (results: Map[RobotId, Seq[TestRun]], time: FiniteDuration) = Benchmark.time {
          Functions.extractTests(content.map(Functions.toStepInfo).collect { case Some(info) => info })
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
            }.drop(1)
            Data(filename, config, robotId, fitnessCurve, tests.map(_.bn))
        }
        System.gc()
        val r = Runtime.getRuntime
        println("Memory usage: " + (r.totalMemory - r.freeMemory) + " of " + r.maxMemory)
        utils.File.write(filename + ".json", Json.prettyPrint(Json.toJson(experimentData)))
    }
  })
}
