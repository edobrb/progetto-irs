package main

import com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker
import model.config.Configuration
import model.{RobotData, StepInfo}
import play.api.libs.json.{Json, OFormat}
import utils.Parallel.Parallel
import model.config.Configuration.JsonFormats._
import org.knowm.xchart.BitmapEncoder
import org.knowm.xchart.BitmapEncoder.BitmapFormat

import java.awt.Color

object Entropy extends App {


  implicit val arguments: Array[String] = args

  case class Result(configuration: Configuration, fitness: Double, entropy: Double, robotId: String)

  implicit def resultFormat: OFormat[Result] = Json.format[Result]

  if (Args.LOAD_OUTPUT) {
    val results: Seq[Result] = Loader.FILENAMES(args).parMap(Args.PARALLELISM_DEGREE, {
      case (gzipFile, jsonFile) =>
        RobotData.loadsFromFile(jsonFile).toOption.map(robotsData => {

          //id -> (config, (fitness,epoch),(toDrop,toTake))
          val maxes = robotsData.map(data => {
            val printOfOneEpoch = data.config.adaptation.epoch_length * data.config.simulation.ticks_per_seconds + 2
            val (bestFitness, bestEpoch) = data.fitness_values.zipWithIndex.maxBy(_._1)
            val toDrop = printOfOneEpoch * bestEpoch
            (data.robot_id, (data.config, (bestFitness, bestEpoch), (toDrop, printOfOneEpoch)))
          }).toMap

          val (lines, source) = utils.File.readGzippedLines(gzipFile).get
          implicit val siCodec: JsonValueCodec[StepInfo] = JsonCodecMaker.make
          val steps: Map[String, Seq[StepInfo]] = lines.map(l => Loader.toStepInfo(l)).collect {
            case Some(v) => v
          }.filter(v => {
            val (_, _, (toDrop, toTake)) = maxes(v.id)
            toDrop <= v.step && v.step < (toDrop + toTake)
          }).toSeq.groupBy(_.id)
          source.close()


          maxes.toSeq.map {
            case (robotId, (config, (fitness, epoch), (_, printOfOneEpoch))) =>
              val bestEpochInputs = steps(robotId).drop(1).dropRight(1).map(_.inputs.take(config.objective.obstacle_avoidance.proximity_nodes))
              val inputsProbabilities = bestEpochInputs.groupBy(identity).map(v => v._2.size.toDouble / (printOfOneEpoch - 2))
              val entropy = utils.Entropy.shannon(inputsProbabilities)
              println((gzipFile.split('-').last, fitness, entropy, robotId))
              Result(config, fitness, entropy, robotId)
          }
        }).getOrElse(Nil)
    }).flatten.toSeq


    val json = Json.toJson(results).toString()
    utils.File.write(s"${Analyzer.RESULT_FOLDER(args)}/entropy-data.json", json)
  }

  if (Args.MAKE_CHARTS) {
    val jsonStr = utils.File.read(s"${Analyzer.RESULT_FOLDER(args)}/entropy-data.json").get
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
        val chart = utils.Charts.scatterPlot(title, "Entropy", "Fitness",
          Seq(("all", Some(new Color(255, 0, 0, 20)), results.map(v => (v.entropy, Math.max(0.0, v.fitness))))),
          _.setLegendVisible(false))
        BitmapEncoder.saveBitmap(chart, s"${Analyzer.RESULT_FOLDER(args)}/entropy-scatterplot-$title.png", BitmapFormat.PNG)
    }
    val chart = utils.Charts.scatterPlot("All", "Entropy", "Fitness",
      Seq(("all", Some(new Color(255, 0, 0, 5)), results.map(v => (v.entropy, Math.max(0.0, v.fitness))))),
      _.setLegendVisible(false))
    BitmapEncoder.saveBitmap(chart, s"${Analyzer.RESULT_FOLDER(args)}/entropy-scatterplot.png", BitmapFormat.PNG)
  }
}
