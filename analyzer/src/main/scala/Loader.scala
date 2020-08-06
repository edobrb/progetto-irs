import model.Types.RobotId
import model.config.Config
import model.config.Config.JsonFormats._
import model.{BooleanNetwork, StepInfo, TestRun}
import play.api.libs.json.{JsError, JsSuccess, Json, OFormat}
import utils.Benchmark
import utils.RichParIterable._
import utils.{Argos, Benchmark}

import scala.collection.parallel.CollectionConverters._
import scala.concurrent.duration.FiniteDuration
import scala.util.{Failure, Success, Try}

object Loader extends App {

  def filenames: Iterable[String] = Experiments.experiments.keys.map(Experiments.DATA_FOLDER + "/" + _)

  case class Data(filename: String, config: Config, robot_id: String, fitness_curve: Seq[Double], fitness_values: Seq[Double], bestBn: BooleanNetwork.Schema)

  implicit def dataFormat: OFormat[Data] = Json.format[Data]
  implicit def siFormat: OFormat[StepInfo] = Json.format[StepInfo]

  def toStepInfo(jsonStep: String): Option[StepInfo] =
    Try(Json.parse(jsonStep)) match {
      case Success(json) => Json.fromJson[StepInfo](json) match {
        case JsSuccess(info, _) => Some(info)
        case JsError(_) => None
      }
      case Failure(_) => None
    }

  def extractTests(data: Iterator[StepInfo], ignoreBnStates: Boolean): Map[RobotId, Seq[TestRun]] =
    data.map(v => if (ignoreBnStates) v.copy(states = Nil) else v).toSeq.groupBy(_.id).map {
      case (id, steps) =>
        (id, steps.toSeq.sortBy(_.step).foldLeft(Seq[TestRun]()) {
          case (l :+ last, StepInfo(step, id, None, states, fitness)) =>
            l :+ (last add(states, fitness))
          case (l :+ last, StepInfo(step, id, Some(bn), states, fitness)) if bn == last.bn =>
            l :+ (last add(states, fitness))
          case (l :+ last, StepInfo(step, id, Some(bn), states, fitness)) if bn != last.bn =>
            l :+ last :+ TestRun(bn, Seq((states, fitness)))
          case (Nil, StepInfo(step, id, Some(bn), states, fitness)) =>
            Seq(TestRun(bn, Seq((states, fitness))))
        })
    }

  filenames/*.toList.par.parallelism(4)*/.foreach(filename => {
    println(s"Loading $filename ... ")
    utils.File.readGzippedLines2(filename) {
      content: Iterator[String] =>
        val config: Config = Config.fromJson(content.next())
        val (results: Map[RobotId, Seq[TestRun]], time: FiniteDuration) = Benchmark.time {
          extractTests(content.map(toStepInfo).collect { case Some(info) => info }, ignoreBnStates = true)
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
