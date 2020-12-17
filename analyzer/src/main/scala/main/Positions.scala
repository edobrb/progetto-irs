package main

import com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker
import model.config.Configuration
import model.{RobotData, StepInfo}
import org.knowm.xchart.BitmapEncoder
import org.knowm.xchart.BitmapEncoder.BitmapFormat
import org.knowm.xchart.style.markers.{Circle, Marker}
import play.api.libs.json.{Json, OFormat}
import utils.Parallel.Parallel

import java.awt.Color

object Positions extends App {


  implicit val arguments: Array[String] = args


  implicit val siCodec: JsonValueCodec[StepInfo] = JsonCodecMaker.make


  val results: Seq[(Configuration, (String, RobotData))] = Loader.FILENAMES(args).parMap(Args.PARALLELISM_DEGREE, {
    case (gzipFile, jsonFile) =>
      println("Loading " + jsonFile)
      val tmpGzipFile = Analyzer.RESULT_FOLDER(args) + "/tmp/" + gzipFile.split('/').last
      RobotData.loadsFromFile(jsonFile).toOption.map(_.map(d => (tmpGzipFile, d)))
  }).collect {
    case Some(value) => value
  }.flatten.groupBy(_._2.config).map {
    case (configuration, value) => (configuration, value.maxBy(_._2.fitness_values.max))
  }.toSeq

  results.groupBy(v =>
    v._1.copy(network = v._1.network.copy(p = 0), adaptation = Settings.selectedExperiment(args).defaultConfig.adaptation)
      .setControllersSeed(None).setSimulationSeed(None)).foreach {
    case (config, results) =>
      /*val steps = results.groupBy(_._1.setControllersSeed(None).setSimulationSeed(None)).map(_._2.maxBy(_._2._2.fitness_values.max)).map {
        case (configuration, (file, robotData)) =>
          println("Loading " + file)
          val (lines, source) = utils.File.readGzippedLines(file).get
          val steps: Seq[StepInfo] = lines.map(l => Loader.toStepInfo(l)).collect {
            case Some(v) => v
          }.toSeq.filter(_.id == robotData.robot_id)
          source.close()
          (configuration, steps, robotData.fitness_values.max)
      }.toSeq.sortBy(_._3).map(v => (v._1, v._2))*/


      val bestRobot = results.maxBy(_._2._2.fitness_values.max)
      val (lines, source) = utils.File.readGzippedLines(bestRobot._2._1).get
      val steps = Seq((bestRobot._1, lines.map(l => Loader.toStepInfo(l)).collect {
        case Some(v) => v
      }.toSeq.filter(_.id == bestRobot._2._2.robot_id)))
      source.close()

      val colors = Seq(
        new Color(0, 0, 0, 128),
        new Color(255, 255, 0, 128),
        new Color(255, 0, 255, 128),
        new Color(0, 255, 255, 128),
        new Color(128, 128, 128, 128),
        new Color(128, 64, 12, 128),
        new Color(0, 0, 255, 128),
        new Color(0, 255, 0, 128),
        new Color(255, 0, 0, 128),
      )

      val series = steps.zipWithIndex.map({ case ((config, s), i) =>
        (s"p=${config.network.p},rewire=${config.adaptation.network_io_mutation.max_input_rewires > 0},mutation=${config.adaptation.network_mutation.max_connection_rewires > 0}",
          Some(colors(i)), s.map(v => (v.location._2, v.location._1)))
      })
      val arena = (config.simulation.argos, config.objective.half_region_variation.isDefined) match {
        case ("experiments/parametrized.argos", false) => "whole"
        case ("experiments/parametrized.argos", true) => "half"
        case ("experiments/parametrized-foraging.argos", false) => "foraging"
        case ("experiments/parametrized-foraging2.argos", false) => "foraging2"
      }
      val title = s"arena=$arena"
      val chart = utils.Charts.scatterPlot(title, "Entropy", "Fitness",
        series,
        s => {
          s.setMarkerSize(8)
          s.setXAxisMax(2)
          s.setXAxisMin(-2)
          s.setYAxisMax(2)
          s.setYAxisMin(-2)
        }, b => b.width(1400).height(900))
      BitmapEncoder.saveBitmap(chart, s"${Analyzer.RESULT_FOLDER(args)}/position-scatterplot-$title.png", BitmapFormat.PNG)
  }
}
