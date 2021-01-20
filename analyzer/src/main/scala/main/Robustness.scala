package main

import utils.ConfigLens._
import com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker
import model.Types.Fitness
import model.config.Configuration
import model.{BooleanNetwork, RobotData, StepInfo}
import org.knowm.xchart.BitmapEncoder
import org.knowm.xchart.BitmapEncoder.BitmapFormat
import play.api.libs.json._
import utils.ConfigLens.lens
import utils.Parallel.Parallel

import scala.util.{Random, Success}
import model.config.Configuration.JsonFormats._

object Robustness extends App {

  implicit val arguments: Array[String] = args

  case class Result(configuration: Configuration, bestFitness: Seq[Fitness], robustnessFitness: Seq[Fitness])

  implicit def resultFormat: OFormat[Result] = Json.format[Result]

  implicit val siCodec: JsonValueCodec[StepInfo] = JsonCodecMaker.make

  def RANDOM_INITIAL_STATE(implicit args: Array[String]): Boolean = true

  def DESTROYED_ARCH(implicit args: Array[String]): Double = 0.0

  def ROBUSTNESS_FOLDER(implicit args: Array[String]): String = s"${Analyzer.RESULT_FOLDER(args)}/${Args.CONFIGURATION(args)}_robustness/rs=${RANDOM_INITIAL_STATE(args)}-da=${DESTROYED_ARCH(args)}"

  def RESULT_FILE(implicit args: Array[String]): String = s"${ROBUSTNESS_FOLDER(args)}/results.json"


  if (utils.Folder.create(ROBUSTNESS_FOLDER).exists(_.isFailure)) {
    println("Cannot create robustness folder")
    System.exit(-1)
  }

  val networkLens = lens(_.network.initial_schema)
  val networkStateLens = lens(_.network.initial_state)
  val lengthLens = lens(_.simulation.experiment_length)
  val robotLens = lens(_.simulation.robot_count)
  val ioLens = lens(_.adaptation.network_io_mutation.max_input_rewires) and lens(_.adaptation.network_io_mutation.max_output_rewires)
  val netLens = lens(_.adaptation.network_mutation.max_connection_rewires) and lens(_.adaptation.network_mutation.max_function_bit_flips)
  val networkPerConfiguration = 10
  val repetitions = 10
  val robotCount = 1
  val epochCount = 10

  val configs = Settings.configurations.map(v => v.filename -> v).toMap


  if (Args.LOAD_OUTPUT) {
    println("Loading files...")
    val robotsData: Iterable[(Configuration, Seq[(Double, Fitness, BooleanNetwork)])] = Loader.OUTPUT_FILENAMES.parMap(Args.PARALLELISM_DEGREE, { filename =>
      RobotData.loadsFromFile(filename).map(robotsData => {
        println("Loaded " + filename)
        robotsData.map {
          case RobotData(robot_id, config, fitness_values, best_network, locations) =>
            (configs(config.filename), (fitness_values.sum, fitness_values.max, best_network))
        }
      })
    }).collect({
      case Success(v) => v
    }).flatten.groupBy(_._1).map {
      case (configuration, value) => (configuration, value.map(_._2).toList.sortBy(-_._1).take(networkPerConfiguration))
    }

    val resultFitness: Map[Configuration, Seq[Fitness]] = robotsData.zipWithIndex.flatMap({
      case ((config, networks), configurationIndex) =>
        val work = networks.flatMap {
          case (_, _, network) => (0 until repetitions).map(i => (network, i))
        }
        work.zipWithIndex.parFlatmap(Args.PARALLELISM_DEGREE, {
          case ((network, i), workIndex) =>
            val newConfig0 = if (DESTROYED_ARCH > 0) {
              val n = config.network.n
              val k = config.network.k
              val archToDestroy = (n * k * DESTROYED_ARCH).toInt
              val newNetwork = (0 until archToDestroy).map(_ => (Random.nextInt(n), Random.nextInt(k))).foldLeft(network)({
                case (network, (node, connection)) => network.copy(connections = network.connections.updated(node, network.connections(node).updated(connection, -1)))
              })
              networkLens.set(Some(newNetwork))(config)
            } else {
              networkLens.set(Some(network))(config)
            }
            val newConfig1 = lengthLens.set(config.adaptation.epoch_length * epochCount)(newConfig0)
            val newConfig2 = robotLens.set(robotCount)(newConfig1)
            val newConfig3 = (ioLens and netLens).set((0, 0), (0, 0))(newConfig2)
            val newConfig4 = if (RANDOM_INITIAL_STATE) {
              newConfig3
            } else {
              networkStateLens.set(Some(network.states))(newConfig3)
            }
            val (output, time) = utils.Benchmark.time(Experiments.runSimulation(newConfig4.setSeed(i), visualization = false).map(v => Loader.toStepInfo(v)).collect {
              case Some(si) => si
            }.toSeq.groupBy(_.id).toSeq.map({
              case (_, steps) => (config, steps.maxBy(_.fitness).fitness)
            }))
            println(s"Finished experiment ${workIndex + configurationIndex * work.size}/${work.size * configs.size} (${time.toMillis} ms)")
            output
        })
    }).groupBy(_._1).map(v => v._1 -> v._2.map(_._2).toSeq)


    val results: Seq[Result] = robotsData.toSeq.map {
      case (configuration, values) =>
        val bestOldFitness: Seq[Fitness] = values.map(_._2)
        val robustnessFitness: Seq[Fitness] = resultFitness(configuration)
        Result(configuration, bestOldFitness, robustnessFitness)
    }

    val json = Json.toJson(results).toString()
    utils.File.write(RESULT_FILE, json)
  }

  if (Args.MAKE_CHARTS) {
    println("plotting charts...")
    val jsonStr = utils.File.read(RESULT_FILE).get
    val json = Json.parse(jsonStr)
    val results = Json.fromJson[Seq[Result]](json).get.sortBy(-_.bestFitness.sum)

    Settings.selectedExperiment.configVariation.filter(!_.collapse).foreach({ v =>
      results.groupBy(r => v.getVariation(r.configuration)).foreach {
        case (_, results: Seq[Result]) =>
          val c = results.head.configuration

          var spaces = 1
          val series: Iterable[(String, Iterable[Double])] = results.flatMap(result => {
            val legend = Settings.selectedExperiment.configVariation.filter(_.name != v.name).map(v => s"${v.name}=${v.desc(result.configuration)}").mkString(",")
            val source: (String, Iterable[Double]) = (legend, result.bestFitness)
            val dest: (String, Iterable[Double]) = ((0 until spaces).map(_ => " ").mkString, result.robustnessFitness)
            spaces = spaces + 1
            Seq[(String, Iterable[Double])](source, dest)
          })

          val chart = utils.Charts.boxplot(s"(with ${v.name}=${v.desc(c)})", "variations", "fitness", series)
          BitmapEncoder.saveBitmap(chart, s"$ROBUSTNESS_FOLDER/${v.name}=${v.desc(c)}.png", BitmapFormat.PNG)
      }
    })
  }
}
