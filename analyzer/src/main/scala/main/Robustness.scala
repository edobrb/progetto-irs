package main

import utils.ConfigLens._
import com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker
import model.Types.Fitness
import model.config.Configuration
import model.{BooleanNetwork, RobotData, StepInfo}
import org.knowm.xchart.{BitmapEncoder, VectorGraphicsEncoder}
import org.knowm.xchart.BitmapEncoder.BitmapFormat
import play.api.libs.json._
import utils.ConfigLens.lens
import utils.Parallel.Parallel

import scala.util.{Random, Success}
import model.config.Configuration.JsonFormats._
import org.knowm.xchart.VectorGraphicsEncoder.VectorGraphicsFormat
import org.knowm.xchart.style.Styler

import java.awt.Color
import java.text.DecimalFormat

object Robustness extends App {

  implicit val arguments: Array[String] = args

  case class Result(configuration: Configuration, bestFitness: Seq[Fitness], robustnessFitness: Seq[Fitness])

  implicit def resultFormat: OFormat[Result] = Json.format[Result]

  implicit val siCodec: JsonValueCodec[StepInfo] = JsonCodecMaker.make

  def RANDOM_INITIAL_STATE(implicit args: Array[String]): Boolean = false

  def DESTROYED_ARCH(implicit args: Array[String]): Double = 0.1

  def ROBOT_COUNT(implicit args: Array[String]): Int = 2

  def EPOCH_COUNT(implicit args: Array[String]): Int = 2

  def ROBUSTNESS_FOLDER(implicit args: Array[String]): String =
    s"${Analyzer.RESULT_FOLDER(args)}/${Args.CONFIGURATION(args)}_robustness/rs=${RANDOM_INITIAL_STATE(args)}-da=${DESTROYED_ARCH(args)}" +
      s"-rc=${ROBOT_COUNT(args)}-ec=${EPOCH_COUNT(args)}"

  def RESULT_FILE(implicit args: Array[String]): String = s"${ROBUSTNESS_FOLDER(args)}/results.json"


  if (utils.Folder.create(ROBUSTNESS_FOLDER).exists(_.isFailure)) {
    println("Cannot create robustness folder")
    System.exit(-1)
  }

  val networkLens = lens(_.network.initial_schema)
  val networkStateLens = lens(_.network.initial_state)
  val lengthLens = lens(_.simulation.experiment_length)
  val epochLengthLens = lens(_.adaptation.epoch_length)
  val robotLens = lens(_.simulation.robot_count)
  val ioLens = lens(_.adaptation.network_io_mutation.max_input_rewires) and lens(_.adaptation.network_io_mutation.max_output_rewires)
  val netLens = lens(_.adaptation.network_mutation.max_connection_rewires) and lens(_.adaptation.network_mutation.max_function_bit_flips)
  val networkPerConfiguration = 100
  val repetitions = 10

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
      case (configuration, value) => (configuration, value.map(_._2).toList.sortBy(-_._2).take(networkPerConfiguration))
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
            val newConfig2 = lengthLens.set(config.adaptation.epoch_length * EPOCH_COUNT)(newConfig0)
            val newConfig3 = robotLens.set(ROBOT_COUNT)(newConfig2)
            val newConfig4 = (ioLens and netLens).set((0, 0), (0, 0))(newConfig3)
            val newConfig5 = if (RANDOM_INITIAL_STATE) {
              newConfig4
            } else {
              networkStateLens.set(Some(network.states))(newConfig4)
            }
            val (output, time) = utils.Benchmark.time(Experiments.runSimulation(newConfig5.setSeed(i), visualization = false).map(v => Loader.toStepInfo(v)).collect {
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
    val results = Json.fromJson[Seq[Result]](json).get/*.filter(_.configuration.network.p != 0.5).*/.sortBy(-_.bestFitness.sum)

    /*val jsonStr1 = utils.File.read(s"${Analyzer.RESULT_FOLDER(args)}/${Args.CONFIGURATION(args)}_robustness/rs=false-da=0.0" + s"-rc=${ROBOT_COUNT(args)}-ec=${EPOCH_COUNT(args)}/results.json").get
    val json1 = Json.parse(jsonStr1)
    val results1 = Json.fromJson[Seq[Result]](json1).get/*.filter(_.configuration.network.p != 0.5).*/.sortBy(-_.bestFitness.sum)

    val results = results2 .map(result => {
      val r1 = results1.find(_.configuration == result.configuration).get
      result.copy(bestFitness = r1.robustnessFitness)
    })*/

    //CSV save
    def variationName(configuration: Configuration): String = {
      val variations = Settings.selectedExperiment.configVariation
      variations.map(v => {
        if (v.legendName.nonEmpty) s"${v.legendName}: ${v.desc(configuration)}" else v.desc(configuration)
      }).mkString(",")
    }
    val resCsv = results.groupBy(_.configuration.setControllersSeed(None).setSimulationSeed(None)).map {
      case (configuration, value) =>
        val columnName = variationName(configuration)
        (columnName, value.flatMap(_.robustnessFitness).toIndexedSeq)
    }.toIndexedSeq
    val header = resCsv.map(_._1).mkString(";")
    val rows = resCsv.foldLeft(Seq[String]()) {
      case (Nil, (_, fitnesses)) => fitnesses.map(_.toString)
      case (lines, (_, fitnesses)) => lines.zip(fitnesses.map(_.toString)).map(v => v._1 + ";" + v._2)
    }
    utils.File.writeLines(s"$ROBUSTNESS_FOLDER/results.csv", header +: rows)

    val formatter = new DecimalFormat("#.#")
    results.map (r => {
      (Settings.selectedExperiment.configVariation.map(_.desc(r.configuration)), - (r.bestFitness.sum / r.bestFitness.size) + (r.robustnessFitness.sum / r.robustnessFitness.size))
    }).groupBy(_._1(2)).foreach { r =>
      println(r._1)
      r._2.toSeq.map({
        case (p :: adattamento :: arena :: _, v) => (p, adattamento, v)
      }).groupBy(_._2).foreach {
        case (adattamento, res) =>
          val resP = res.groupBy(_._1)
          println(adattamento + " & " + formatter.format(resP("0.1").head._3) + " & " + formatter.format(resP("0.79").head._3) + " & " + formatter.format(resP("0.5").head._3))
      }
      println("")
    }


    Settings.selectedExperiment.configVariation.filter(!_.collapse).foreach({ v =>
      results.groupBy(r => v.getVariation(r.configuration)).foreach {
        case (_, results: Seq[Result]) =>
          val c = results.head.configuration

          var spaces = 1
          val series: Iterable[(String, Iterable[Double])] = results.sortBy({
            case Result(configuration, bestFitness, robustnessFitness) =>
              (configuration.network.p match {
                case 0.79 => 10
                case 0.1 => 20
                case _ => 30
              }) + ((configuration.adaptation.network_mutation.max_connection_rewires, configuration.adaptation.network_io_mutation.max_input_rewires) match {
              case (m, s) if m > 0 && s == 0 => 3
              case (m, s) if m > 0 && s > 0 => 2
              case _ => 1
            })
          }).flatMap(result => {
            val legend = Settings.selectedExperiment.configVariation.filter(_.name != v.name).map(v =>
              if(v.legendName.nonEmpty) s"${v.legendName}: ${v.desc(result.configuration)}" else v.desc(result.configuration)).mkString(",")
            val source: (String, Iterable[Double]) = (legend, result.bestFitness)
            val dest: (String, Iterable[Double]) = ((0 until spaces).map(_ => " ").mkString, result.robustnessFitness)
            spaces = spaces + 1
            Seq[(String, Iterable[Double])](source, dest)
          })

          val chart = utils.Charts.boxplot(s"${v.legendName} ${v.desc(c)}", "variations", "Fitness massima", series,
            applyCustomStyle = v => {
            v.setPlotContentSize(0.96)
            v.setMarkerSize(4)
            v.setXAxisLabelRotation(90)
              v.setXAxisLabelAlignment(Styler.TextAlignment.Left)
              v.setXAxisLabelAlignmentVertical(Styler.TextAlignment.Left)
            v.setXAxisTitleVisible(false)
            v.setChartBackgroundColor(Color.white)
            v.setChartTitleVisible(true)
          },
            applyCustomBuild = _.width(600).height(600))
          BitmapEncoder.saveBitmap(chart, s"$ROBUSTNESS_FOLDER/${v.name}=${v.desc(c)}.png", BitmapFormat.PNG)
          VectorGraphicsEncoder.saveVectorGraphic(chart,s"$ROBUSTNESS_FOLDER/${v.name}=${v.desc(c)}.pdf", VectorGraphicsFormat.PDF)
      }
    })
  }
}
