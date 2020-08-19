import model.Types.RobotId
import model.config.Config
import model.config.Config.JsonFormats._
import model.{RobotData, StepInfo, TestRun}
import play.api.libs.json.{JsError, JsSuccess, Json, OFormat}
import utils.Benchmark
import utils.Parallel._

import scala.concurrent.duration.FiniteDuration
import scala.util.{Failure, Success, Try}

object Loader extends App {

  def INPUT_FILENAMES: Iterable[String] = Experiments.experiments.keys.map(Experiments.DATA_FOLDER + "/" + _)

  def OUTPUT_FILENAMES: Iterable[String] = FILENAMES.map(_._2)

  def FILENAMES: Iterable[(String, String)] = INPUT_FILENAMES.map(v => (v, v + ".json"))

  /** Formats for json conversions **/
  implicit def dataFormat: OFormat[RobotData] = Json.format[RobotData]

  implicit def siFormat: OFormat[StepInfo] = Json.format[StepInfo]

  /** Map a experiment output's line to it's information (StepInfo) **/
  def toStepInfo(jsonStep: String): Option[StepInfo] =
    Try(Json.parse(jsonStep)) match {
      case Success(json) => Json.fromJson[StepInfo](json) match {
        case JsSuccess(info, _) => Some(info)
        case JsError(_) => None
      }
      case Failure(_) => None
    }

  /** Map a whole experiment into a map of [robot id -> sequence of tests information] **/
  def extractTests(data: Iterator[StepInfo], ignoreBnStates: Boolean): Map[RobotId, Seq[TestRun]] =
    data.map(v => if (ignoreBnStates) v.copy(states = Nil) else v).toSeq.groupBy(_.id).map {
      case (id, steps) =>
        (id, steps.sortBy(_.step).foldLeft(Seq[TestRun]()) {
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

  /** Run the loader. Foreach experiments executes "extractTests" then map each experiment into a sequence of
   * RobotData and then writes it into a json file **/
  FILENAMES.toList.sortBy(_._1).parForeach(threads = 4, {
    case (input_filename, output_filename) if !utils.File.exists(output_filename) && utils.File.exists(input_filename) =>
      println(s"Loading $input_filename ... ")
      utils.File.readGzippedLines2(input_filename) {
        content: Iterator[String] =>
          val config: Config = Config.fromJson(content.next())
          val (results: Map[RobotId, Seq[TestRun]], time: FiniteDuration) = Benchmark.time {
            extractTests(content.map(toStepInfo).collect { case Some(info) => info }, ignoreBnStates = true)
          }
          println(s"done. (${time.toSeconds} s)")
          (config, results)
      } match {
        case Failure(exception) => println(s"$input_filename throw an exception: ${exception.getMessage}"); Seq()
        case Success((config: Config, result: Map[RobotId, Seq[TestRun]])) =>
          val robotsData = result.map {
            case (robotId, tests) =>
              RobotData(input_filename, config, robotId, tests.map(_.fitnessValues.last), tests.maxBy(_.fitnessValues.last).bn)
          }
          utils.File.write(output_filename, Json.prettyPrint(Json.toJson(robotsData)))
      }
    case (input_filename, output_filename) if utils.File.exists(output_filename) => println("Skipping " + input_filename)
    case (input_filename, _) => println("Not found " + input_filename)
  })
}
