package main

import java.awt.Color
import com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker
import model.{RobotData, StepInfo}
import model.config.Configuration
import org.knowm.xchart.BitmapEncoder
import org.knowm.xchart.BitmapEncoder.BitmapFormat
import org.knowm.xchart.style.markers.{Circle, Marker}
import play.api.libs.json._
import utils.Parallel.Parallel

import scala.util.{Failure, Success, Try}
import model.config.Configuration.JsonFormats._
import org.knowm.xchart.style.Styler.LegendPosition

object Derrida extends App {

  implicit val arguments: Array[String] = args

  case class Result(configuration: Configuration, fitness: Double, derrida: Double, robotId: String, fromFile: String)

  implicit def resultFormat: OFormat[Result] = Json.format[Result]

  implicit val siCodec: JsonValueCodec[StepInfo] = JsonCodecMaker.make

  def DERRIDA_FOLDER(implicit args: Array[String]): String = s"${Analyzer.RESULT_FOLDER(args)}/${Args.CONFIGURATION(args)}_derrida"

  if (utils.Folder.create(DERRIDA_FOLDER).exists(_.isFailure)) {
    println("Cannot create derrida folder")
    System.exit(-1)
  }

  def loadResults: Seq[Result] = Try {
    val jsonStr = utils.File.read(s"$DERRIDA_FOLDER/results.json").get
    val json = Json.parse(jsonStr)
    Json.fromJson[Seq[Result]](json).get
  }.getOrElse(Nil)

  if (Args.LOAD_OUTPUT) {
    val loaded = loadResults
    println("Loaded: " + loaded.size)
    val loadedFile = loaded.map(v => v.fromFile -> ()).toMap
    val results: Seq[Result] = loaded ++ Loader.FILENAMES(args).filter(v => !loadedFile.contains(v._1.split('/').last)).parMap(Args.PARALLELISM_DEGREE, {
      case (gzipFile, jsonFile) =>
        val tmpGzipFile = LoadBest.BEST_RAW_FOLDER + "/" + gzipFile.split('/').last
        RobotData.loadsFromFile(jsonFile).toOption.flatMap(robotsData => {

          //id -> (config, (fitness,epoch),(toDrop,toTake))
          val maxes = robotsData.map(data => {
            val printOfOneEpoch = data.config.adaptation.epoch_length * data.config.simulation.ticks_per_seconds + 2
            val (bestFitness, bestEpoch) = data.fitness_values.zipWithIndex.maxBy(_._1)
            val toDrop = printOfOneEpoch * bestEpoch
            (data.robot_id, (data.config, (bestFitness, bestEpoch), (toDrop, printOfOneEpoch)))
          }).toMap

          utils.File.readGzippedLinesAndMap(tmpGzipFile)(lines => {
            val steps: Map[String, Seq[StepInfo]] = lines.map(l => Loader.toStepInfo(l)).collect {
              case Some(v) => v
            }.toSeq.groupBy(_.id)

            maxes.toSeq.map {
              case (robotId, (config, (fitness, _), (_, _))) =>
                val bestNetwork = steps(robotId).head.boolean_network.get
                val bns = steps(robotId).drop(1).dropRight(1).scanLeft(bestNetwork)({
                  case (bn, stepInfo) => bn.withInputs(stepInfo.inputs).step()
                })

                val derridaValues = bns.groupBy(_.states).map({ case (_, networks) =>
                  val bn = networks.head
                  val bnNext = bn.step()
                  bn.states.indices.map(i =>
                    bn.invertState(i).step().statesHammingDistance(bnNext)
                  ).sum.toDouble / bn.states.size * networks.size
                })
                val derrida = derridaValues.sum / bns.size

                println((gzipFile.split('-').last, fitness, derrida, robotId))
                Result(config, fitness, derrida, robotId, gzipFile.split('/').last)
            }
          }) match {
            case Failure(exception) => println(s"Error: ${exception.getMessage}"); None
            case Success(value) => Some(value)
          }
        }).getOrElse(Nil)
    }).flatten.toSeq

    println("saving results...")
    val json = Json.toJson(results).toString()
    utils.File.write(s"$DERRIDA_FOLDER/results.json", json)
  }


  if (Args.MAKE_CHARTS) {
    val loaded = loadResults
    println("Loaded: " + loaded.size)
    println("Plotting charts...")
    loaded.groupBy(v => v.configuration.copy(network = v.configuration.network.copy(p = 0)).setControllersSeed(None).setSimulationSeed(None)).foreach {
      case (config, data) =>
        val series = data.groupBy(_.configuration.network.p).zip(Seq(new Color(255, 0, 0), new Color(0, 255, 0), new Color(0, 0, 255))).map {
          case ((p, data), color) => (s"p=$p", Some(new Color(color.getRed, color.getGreen, color.getBlue, 30)), data.map(v => (v.derrida, Math.max(0.0, v.fitness))))
        }
        val mutation = config.adaptation.network_mutation.max_connection_rewires > 0
        val rewire = config.adaptation.network_io_mutation.max_input_rewires > 0
        val arena = (config.simulation.argos, config.objective.half_region_variation.isDefined) match {
          case ("experiments/parametrized.argos", false) => "whole"
          case ("experiments/parametrized.argos", true) => "half"
          case ("experiments/parametrized-foraging.argos", false) => "foraging"
          case ("experiments/parametrized-foraging2.argos", false) => "foraging2"
        }
        val title = s"arena=$arena-rewire=$rewire-mutation=$mutation"
        val chart = utils.Charts.scatterPlot(title, "Derrida", "Fitness",
          series,
          s => {
            s.setChartTitleVisible(false)
            s.setMarkerSize(4)
            s.setXAxisMax(2)
            s.setXAxisMin(0)
            s.setYAxisMax(400)
            s.setChartBackgroundColor(Color.WHITE)
            s.setLegendPosition(LegendPosition.InsideNE)
            s.setSeriesMarkers(Seq[Marker](new Circle(), new Circle(), new Circle()).toArray)
          },
          s => {
            s.width(800)
            s.height(600)
          })
        BitmapEncoder.saveBitmap(chart, s"$DERRIDA_FOLDER/$title.png", BitmapFormat.PNG)
    }
  }
}
