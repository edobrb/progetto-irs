package main

import com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker
import model.config.Configuration
import model.{RobotData, StepInfo}
import play.api.libs.json.{Json, OFormat}
import utils.Parallel.Parallel
import model.config.Configuration.JsonFormats._
import org.knowm.xchart.{BitmapEncoder, VectorGraphicsEncoder}
import org.knowm.xchart.BitmapEncoder.BitmapFormat
import org.knowm.xchart.VectorGraphicsEncoder.VectorGraphicsFormat

import java.awt.Color
import scala.util.{Failure, Success}

object Entropy extends App {

  implicit val arguments: Array[String] = args

  case class Result(configuration: Configuration, fitness: Double, entropy: Double, robotId: String)

  implicit def resultFormat: OFormat[Result] = Json.format[Result]

  implicit val siCodec: JsonValueCodec[StepInfo] = JsonCodecMaker.make

  def ENTROPY_FOLDER(implicit args: Array[String]): String = s"${Analyzer.RESULT_FOLDER(args)}/${Args.CONFIGURATION(args)}_entropy"

  if (utils.Folder.create(ENTROPY_FOLDER + "/png", ENTROPY_FOLDER + "/csv").exists(_.isFailure)) {
    println("Cannot create entropy folder")
    System.exit(-1)
  }

  if (Args.LOAD_OUTPUT) {
    val results: Seq[Result] = Loader.FILENAMES(args).parMap(Args.PARALLELISM_DEGREE, {
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
              case (robotId, (config, (fitness, epoch), (_, printOfOneEpoch))) =>
                val bestEpochInputs = steps(robotId).drop(1).dropRight(1).map(_.inputs.take(config.objective.obstacle_avoidance.proximity_nodes))
                val inputsProbabilities = bestEpochInputs.groupBy(identity).map(v => v._2.size.toDouble / (printOfOneEpoch - 2))
                val entropy = utils.Entropy.shannon(inputsProbabilities)
                println((gzipFile.split('-').last, fitness, entropy, robotId))
                Result(config, fitness, entropy, robotId)
            }
          }) match {
            case Failure(exception) => println(s"Error: ${exception.getMessage}"); None
            case Success(value) => Some(value)
          }
        }).getOrElse(Nil)
    }).flatten.toSeq


    val json = Json.toJson(results).toString()
    utils.File.write(s"$ENTROPY_FOLDER/results.json", json)
  }

  if (Args.MAKE_CHARTS) {
    println(s"Plotting charts at $ENTROPY_FOLDER")
    val jsonStr = utils.File.read(s"$ENTROPY_FOLDER/results.json").get
    val json = Json.parse(jsonStr)
    val results = Json.fromJson[Seq[Result]](json).get
    println(results.size)

    results.groupBy(v =>
      v.configuration.copy(network = v.configuration.network.copy(p = 0), adaptation = Settings.selectedExperiment(args).defaultConfig.adaptation)
        .setControllersSeed(None).setSimulationSeed(None)).foreach {
      case (config, results) =>
        val arena = (config.simulation.argos, config.objective.half_region_variation.isDefined) match {
          case ("experiments/parametrized.argos", false) => "whole"
          case ("experiments/parametrized.argos", true) => "half"
          case ("experiments/parametrized-foraging.argos", false) => "foraging"
          case ("experiments/parametrized-foraging2.argos", false) => "foraging2"
        }
        val title = s"arena=$arena"
        val series = results.map(v => (v.entropy, Math.max(0.0, v.fitness)))
        val chart = utils.Charts.scatterPlot(title, "Entropy", "Fitness",
          Seq(("all", Some(new Color(255, 0, 0, 20)), series)),
          _.setLegendVisible(false).setChartTitleVisible(false).setChartBackgroundColor(Color.WHITE),
          _.width(800).height(600))
        BitmapEncoder.saveBitmap(chart, s"$ENTROPY_FOLDER/png/$title.png", BitmapFormat.PNG)
        //VectorGraphicsEncoder.saveVectorGraphic(chart, s"$ENTROPY_FOLDER/$title.pdf", VectorGraphicsFormat.PDF)
        //VectorGraphicsEncoder.saveVectorGraphic(chart, s"$ENTROPY_FOLDER/$title.svg", VectorGraphicsFormat.SVG)
        val data = "entropy;fitness" +: series.map(v => "%.3f;%.3f".format(v._1, v._2))
        utils.File.writeLines(s"$ENTROPY_FOLDER/csv/$title.csv", data)

    }
    val chart = utils.Charts.scatterPlot("All", "Entropy", "Fitness",
      Seq(("all", Some(new Color(255, 0, 0, 5)), results.map(v => (v.entropy, Math.max(0.0, v.fitness))))),
      _.setLegendVisible(false))
    BitmapEncoder.saveBitmap(chart, s"${Analyzer.RESULT_FOLDER(args)}/entropy-scatterplot.png", BitmapFormat.PNG)
  }
}
