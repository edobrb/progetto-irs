import model.Types.RobotId
import model.config.Configuration
import model.{RobotData, StepInfo, Epoch}
import play.api.libs.json._
import utils.Benchmark
import utils.Parallel._

import scala.concurrent.duration.FiniteDuration
import scala.util.{Failure, Success, Try}
import com.github.plokhotnyuk.jsoniter_scala.macros._
import com.github.plokhotnyuk.jsoniter_scala.core._
import model.config.Configuration.JsonFormats._
import utils.RichIterator.RichIterator

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
  def extractTests(data: Iterator[StepInfo], ignoreBnStates: Boolean): Map[RobotId, Seq[Epoch]] =
    data.map(v => if (ignoreBnStates) v.copy(states = Nil) else v).to(LazyList).groupBy(_.id).map {
      case (robotId, robotSteps) =>
        val (_, performedTests) = robotSteps.foldLeft((-1, Seq[Epoch]())) {
          case ((oldStep, _), StepInfo(step, _, _, _, _, _)) if oldStep >= step => throw new Exception("Unordered steps")
          case ((_, l :+ last), StepInfo(step, _, None, states, fitness, position)) =>
            (step, l :+ last.add(states, fitness, position))
          case ((_, tests), StepInfo(step, _, Some(bn), states, fitness, position)) =>
            (step, tests :+ Epoch(bn, states, fitness, position))
        }
        (robotId, performedTests)
    }

  def extractTests2(data: Iterator[StepInfo], configuration: Configuration, ignoreBnStates: Boolean): Seq[RobotData] = {

    data.foldLeft(Map[RobotId, (RobotData, Option[model.BooleanNetwork.Schema])]())({
      case (map, StepInfo(step, id, None, states, fitness, position)) if map.contains(id) =>
        val (oldData, currentBn) = map(id)
        val newFitnessValues = oldData.fitness_values.dropRight(1) :+ Math.max(oldData.fitness_values.last, fitness)
        val data = oldData.copy(fitness_values = newFitnessValues/*, location = oldData.location :+ step.location*/)
        val data2 = if(currentBn.isDefined && !currentBn.contains(data.best_network) && oldData.fitness_values.forall(_ < fitness)) {
          data.copy(best_network = currentBn.get)
        } else {
          data
        }
        map.updated(id, (data2, currentBn))
      case (map, StepInfo(step, id, Some(bn), states, fitness, position)) if map.contains(id) =>
        val (oldData, _) = map(id)
        val data = oldData.copy(fitness_values = oldData.fitness_values :+ fitness)
        map.updated(id, (data, Some(bn)))
      case (map, StepInfo(step, id, Some(bn), states, fitness, position)) =>
        val data = RobotData("", id, configuration, Seq(fitness), bn, Nil, Nil)
        map.updated(id, (data, None))
    }).values.map(_._1).toSeq

  }

  def load(content: Iterator[String], output_filename: String): Try[FiniteDuration] = {
    Try {
      implicit val siCodec: JsonValueCodec[StepInfo] = JsonCodecMaker.make
      val config: Configuration = Configuration.fromJson(content.next())
      val (robotsData: Iterable[RobotData], time: FiniteDuration) = Benchmark.time {
        val data = content.map(toStepInfo).collect { case Some(info) => info }
        extractTests2(data, config, ignoreBnStates = false)
        /*val results: Map[RobotId, Seq[Epoch]] = tests.map { case (id, value) => (id, value.filter(_.states.size >= config.adaptation.epoch_length)) }

        results.map {
          case (robotId, epochs: Seq[Epoch]) =>
            RobotData("", robotId, config, epochs.map(_.fitnessValues.last),
              epochs.maxBy(_.fitnessValues.last).bn, epochs.maxBy(_.fitnessValues.last).bnStates.last,
              /*epochs.flatMap(_.locations)*/)
        }*/
      }
      utils.File.write(output_filename, Json.prettyPrint(Json.toJson(robotsData)))
      time
    }
  }

  /** Run the loader. Foreach experiments executes "extractTests" then map each experiment into a sequence of
   * RobotData and then writes it into a json file */
  FILENAMES.toList.parForeach(threads = Settings.PARALLELISM_DEGREE, {
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
