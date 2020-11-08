import model.Types.RobotId
import model.config.Config
import model.{RobotData, StepInfo, TestRun}
import play.api.libs.json._
import utils.Benchmark
import utils.Parallel._
import Config.JsonFormats._

import scala.concurrent.duration.FiniteDuration
import scala.util.{Failure, Success, Try}
import com.github.plokhotnyuk.jsoniter_scala.macros._
import com.github.plokhotnyuk.jsoniter_scala.core._

object Loader extends App {

  implicit val arguments: Array[String] = args

  def BASE_FILENAMES(implicit args: Array[String]): Iterable[String] = Settings.experiments(args).sortBy(_._3).map(_._1).map(Settings.DATA_FOLDER(args) + "/" + _)

  def INPUT_FILENAMES(implicit args: Array[String]): Iterable[String] = FILENAMES(args).map(_._1)

  def OUTPUT_FILENAMES(implicit args: Array[String]): Iterable[String] = FILENAMES(args).map(_._2)

  def FILENAMES(implicit args: Array[String]): Iterable[(String, String)] = BASE_FILENAMES(args).map(v => (v + ".gzip", v + ".json"))

  /** Formats for json conversions */
  implicit def dataFormat: OFormat[RobotData] = Json.format[RobotData]

  implicit def siFormat: OFormat[StepInfo] = Json.format[StepInfo]

  /** Map a experiment output's line to it's representation object (StepInfo) */
  def toStepInfo(jsonStep: String)(implicit siCodec: JsonValueCodec[StepInfo]): Option[StepInfo] = {
    Try(readFromString[StepInfo](jsonStep)(siCodec)).toOption
  }

  /** Map a whole experiment into a map of [robot id -> sequence of tests information] */
  def extractTests(data: Iterator[StepInfo], ignoreBnStates: Boolean): Map[RobotId, Seq[TestRun]] =
    data.map(v => if (ignoreBnStates) v.copy(states = Nil) else v).toSeq.groupBy(_.id).map {
      case (id, steps) =>
        (id, steps.sortBy(_.step).foldLeft(Seq[TestRun]()) {
          case (l :+ last, StepInfo(_, _, None, states, fitness, position)) =>
            l :+ last.add(states, fitness, position)
          case (tests, StepInfo(_, _, Some(bn), states, fitness, position)) =>
            tests :+ TestRun(bn, states, fitness, position)
        })
    }

  def load(content: Iterator[String], output_filename: String): Try[FiniteDuration] = {
    Try {
      implicit val siCodec: JsonValueCodec[StepInfo] = JsonCodecMaker.make
      val config: Config = Config.fromJson(content.next())
      val (robotsData: Iterable[RobotData], time: FiniteDuration) = Benchmark.time {
        val data = content.map(toStepInfo).collect { case Some(info) => info }
        val tests = extractTests(data.iterator, ignoreBnStates = true)
        val results: Map[RobotId, Seq[TestRun]] = tests.map { case (id, value) => (id, value.filter(_.states.size >= config.simulation.network_test_steps)) }

        results.map {
          case (robotId, tests) =>
            RobotData("", config, robotId, tests.map(_.fitnessValues.last) /*, tests.flatMap(_.positions)*/ , tests.maxBy(_.fitnessValues.last).bn)
        }
      }
      utils.File.write(output_filename, Json.prettyPrint(Json.toJson(robotsData)))
      time
    }
  }

  /** Run the loader. Foreach experiments executes "extractTests" then map each experiment into a sequence of
   * RobotData and then writes it into a json file */
  FILENAMES.toList.sortBy(_._1).parForeach(threads = Settings.PARALLELISM_DEGREE, {
    case (input_filename, output_filename) if !utils.File.exists(output_filename) && utils.File.exists(input_filename) =>
      println(s"Loading $input_filename ... ")
      utils.File.readGzippedLinesAndMap(input_filename) {
        content: Iterator[String] => load(content, output_filename)
      }.flatten match {
        case Failure(exception) => println(s"$input_filename throw an exception: ${exception.getMessage}"); Seq()
        case Success(time) => println(s"Loading of $input_filename done in ${time.toSeconds} s")
      }
    case (input_filename, output_filename) if utils.File.exists(output_filename) => println("Skipping " + input_filename)
    case (input_filename, _) => println("Not found " + input_filename)
  })

}
