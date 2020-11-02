import model.Types.RobotId
import model.config.Config
import model.{RobotData, StepInfo, TestRun}
import play.api.libs.json._
import utils.Benchmark
import utils.Parallel._
import Config.JsonFormats._

import scala.concurrent.duration.FiniteDuration
import scala.util.{Failure, Success, Try}

object Loader extends App {

  implicit val arguments: Array[String] = args

  def BASE_FILENAMES(implicit args: Array[String]):Iterable[String] = Settings.experiments.keys.map(Settings.DATA_FOLDER(args) + "/" + _ )

  def INPUT_FILENAMES(implicit args: Array[String]): Iterable[String] = FILENAMES(args).map(_._1)

  def OUTPUT_FILENAMES(implicit args: Array[String]): Iterable[String] = FILENAMES(args).map(_._2)

  def FILENAMES(implicit args: Array[String]): Iterable[(String, String)] = BASE_FILENAMES(args).map(v => (v + ".gzip", v + ".json"))

  /** Formats for json conversions */
  implicit def dataFormat: OFormat[RobotData] = Json.format[RobotData]

  implicit def siFormat: OFormat[StepInfo] = Json.format[StepInfo]

  /** Map a experiment output's line to it's representation object (StepInfo) */
  def toStepInfo(jsonStep: String): Option[StepInfo] =
    Try(Json.parse(jsonStep)) match {
      case Success(json) => Json.fromJson[StepInfo](json) match {
        case JsSuccess(info, _) => Some(info)
        case JsError(_) => None
      }
      case Failure(_) => None
    }

  /** Map a whole experiment into a map of [robot id -> sequence of tests information] */
  def extractTests(data: Iterator[StepInfo], ignoreBnStates: Boolean): Map[RobotId, Seq[TestRun]] =
    data.map(v => if (ignoreBnStates) v.copy(states = Nil) else v).toSeq.groupBy(_.id).map {
      case (id, steps) =>
        (id,  steps.sortBy(_.step).foldLeft(Seq[TestRun]()) {
          case (l :+ last, StepInfo(step, id, None, states, fitness, position)) =>
            l :+ last.add(states, fitness, position)
          case (l :+ last, StepInfo(step, id, Some(bn), states, fitness, position)) =>
            l :+ last :+ TestRun(bn, states, fitness, position)
          case (Nil, StepInfo(step, id, Some(bn), states, fitness, position)) =>
            Seq(TestRun(bn, states, fitness, position))
        })
    }


  /** Run the loader. Foreach experiments executes "extractTests" then map each experiment into a sequence of
   * RobotData and then writes it into a json file */
  FILENAMES.toList.sortBy(_._1).parForeach(threads = Settings.PARALLELISM_DEGREE, {
    case (input_filename, output_filename) if !utils.File.exists(output_filename) && utils.File.exists(input_filename) =>
      println(s"Loading $input_filename ... ")
      utils.File.readGzippedLinesAndMap(input_filename) {
        content: Iterator[String] =>
          val config: Config = Config.fromJson(content.next())
          val (results: Map[RobotId, Seq[TestRun]], time: FiniteDuration) = Benchmark.time {
            val data = content.map(toStepInfo).collect { case Some(info) => info }
            val res = extractTests(data, ignoreBnStates = true).map {
              case (id, value) => (id, value.filter(_.states.size >= config.simulation.network_test_steps))
            }
            res
          }
          println(s"done. (${time.toSeconds} s)")
          (config, results)
      } match {
        case Failure(exception) => println(s"$input_filename throw an exception: ${exception.getMessage}"); Seq()
        case Success((config: Config, result: Map[RobotId, Seq[TestRun]])) =>
          val robotsData = result.map {
            case (robotId, tests) =>
              RobotData(input_filename, config, robotId, tests.map(_.fitnessValues.last) /*, tests.flatMap(_.positions)*/ , tests.maxBy(_.fitnessValues.last).bn)
          }
          utils.File.write(output_filename, Json.prettyPrint(Json.toJson(robotsData)))
      }
    case (input_filename, output_filename) if utils.File.exists(output_filename) => println("Skipping " + input_filename)
    case (input_filename, _) => println("Not found " + input_filename)
  })

}
