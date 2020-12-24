package main

import utils.ConfigLens._
import java.awt.Color
import com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker
import model.Types.Fitness
import model.config.Configuration
import model.{BooleanNetwork, RobotData, StepInfo}
import org.knowm.xchart.BitmapEncoder
import org.knowm.xchart.BitmapEncoder.BitmapFormat
import org.knowm.xchart.style.markers.{Circle, Marker}
import play.api.libs.json._
import utils.ConfigLens.lens
import utils.Parallel.Parallel

import scala.util.{Success, Try}
import model.config.Configuration.JsonFormats._

object Robustness extends App {

  implicit val arguments: Array[String] = args

  case class Result(configuration: Configuration, fitness: Double, derrida: Double, robotId: String, fromFile: String)

  implicit def resultFormat: OFormat[Result] = Json.format[Result]

  implicit val siCodec: JsonValueCodec[StepInfo] = JsonCodecMaker.make

  val networkLens = lens(_.network.initial_schema)
  val lengthLens = lens(_.simulation.experiment_length)
  val robotLens = lens(_.simulation.robot_count)
  val ioLens = lens(_.adaptation.network_io_mutation.max_input_rewires) and lens(_.adaptation.network_io_mutation.max_output_rewires)
  val netLens = lens(_.adaptation.network_mutation.max_connection_rewires) and lens(_.adaptation.network_mutation.max_function_bit_flips)
  println("Loading files...")
  val networkPerConfiguration = 10
  val repetitions = 10

  val configs = Settings.configurations(args).map(v => v.filename -> v).toMap
  val robotsData: Iterable[(Configuration, Seq[(Fitness, BooleanNetwork)])] = Loader.OUTPUT_FILENAMES(args).parMap(Args.PARALLELISM_DEGREE(args), { filename =>
    RobotData.loadsFromFile(filename).map(robotsData => {
      println("Loaded " + filename)
      robotsData.map {
        case RobotData(robot_id, config, fitness_values, best_network, locations) =>
          (configs(config.filename), (fitness_values.max, best_network))
      }
    })
  }).collect({
    case Success(v) => v
  }).flatten.groupBy(_._1).map {
    case (configuration, value) => (configuration, value.map(_._2).toList.sortBy(-_._1).take(networkPerConfiguration))
  }

  val fitness: Iterable[(Configuration, Fitness)] = robotsData.parFlatmap(Args.PARALLELISM_DEGREE(args), {
    case (config, networks) =>
      networks.flatMap {
        case (fitness, network) =>
          val newConfig = (ioLens and netLens).set((0, 0), (0, 0))(robotLens.set(10)(lengthLens.set(80 * 10)(networkLens.set(Some(network))(config))))
          (0 until repetitions).flatMap(i => {
            println(s"Running ${newConfig.filename}-$i")
            Experiments.runSimulation(newConfig.setSeed(i), visualization = false)(args).map(v => Loader.toStepInfo(v)).collect {
              case Some(si) => si
            }.toSeq.groupBy(_.id).map({
              case (_, steps) => (config, steps.maxBy(_.fitness).fitness)
            })
          })
      }
  })

  val source = robotsData.map {
    case (config, networks) => config -> networks.map(_._1).sum / networks.size
  }.toMap
  val dest = fitness.groupBy(_._1).map {
    case (configuration, values) => configuration -> (source(configuration), values.map(_._2).sum / values.size)
  }
  println(dest.map(v => v._1.filename -> v._2).mkString("\n"))

}
