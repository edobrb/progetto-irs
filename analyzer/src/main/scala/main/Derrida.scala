package main

import model.RobotData
import model.config.Configuration
import model.config.Configuration.JsonFormats
import org.knowm.xchart.BitmapEncoder.BitmapFormat
import org.knowm.xchart.style.markers.{Circle, Marker}
import org.knowm.xchart.{BitmapEncoder, XYChartBuilder}
import play.api.libs.json.{JsArray, JsSuccess, JsValue, Json, Reads}
import utils.Parallel.Parallel

import java.awt.{Color, Font}
import scala.collection.MapView
import scala.util.Success

object Derrida extends App {

  implicit val arguments: Array[String] = args

  if (Args.LOAD_OUTPUT) {
    println("Loading files...")
    val robotsData: MapView[Configuration, Seq[(Double, Double)]] = Loader.OUTPUT_FILENAMES(args).parMap(Args.PARALLELISM_DEGREE(args), { filename =>
      println(filename)
      RobotData.loadsFromFile(filename).map(_.map {
        case RobotData(robot_id, config, fitness_values, best_network, locations) =>
          val bn = best_network
          val derrida = (0 until 1000).map(i => {
            bn.invertRandomStates(1).steps(1).statesHammingDistance(bn.steps(1))
          })
          (config, fitness_values.max, derrida.sum.toDouble / derrida.size)
      })
    }).collect {
      case Success(v) => v
    }.flatten.groupBy(_._1).view.mapValues(v => v.map(d => (d._2, d._3)).toSeq)

    import JsonFormats._
    val json = Json.toJson(robotsData).toString()
    utils.File.write(s"${Analyzer.RESULT_FOLDER(args)}/derrida-data.json", json)
  }

  if (Args.MAKE_CHARTS) {
    println("Drawing charts...")
    val jsonStr = utils.File.read(s"${Analyzer.RESULT_FOLDER(args)}/derrida-data.json").get
    val json = Json.parse(jsonStr)
    import JsonFormats._
    implicit val myMapRead: Reads[Map[Configuration, Seq[(Double, Double)]]] = (json: JsValue) => JsSuccess {
      json.as[JsArray].value.map { a =>
        val k :: v :: Nil = a.as[JsArray].value.map(_.toString()).toSeq
        (Json.fromJson[Configuration](Json.parse(k)).get, Json.fromJson[Seq[(Double, Double)]](Json.parse(v)).get)
      }.toMap
    }
    val robotsData = Json.fromJson[Map[Configuration, Seq[(Double, Double)]]](json).get
    val data = robotsData.flatMap({
      case (configuration, value) => value.map(v => (configuration, v._1, v._2))
    })
    data.groupBy(v => v._1.copy(network = v._1.network.copy(p = 0)).setControllersSeed(None).setSimulationSeed(None)).foreach {
      case (config, data) =>

        val series = data.groupBy(_._1.network.p).zip(Seq(new Color(255, 0, 0), new Color(0, 255, 0), new Color(0, 0, 255))).map {
          case ((p, data), color) => (s"p=$p", Some(new Color(color.getRed, color.getGreen, color.getBlue, 30)), data.map(v => (v._3, Math.max(0.0, v._2))))
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
            s.setMarkerSize(8)
            s.setXAxisMax(2)
            s.setXAxisMin(0)
            s.setYAxisMax(200)
            s.setSeriesMarkers(Seq[Marker](new Circle(), new Circle(), new Circle()).toArray)
          })
        BitmapEncoder.saveBitmap(chart, s"${Analyzer.RESULT_FOLDER(args)}/derrida-scatterplot-$title.png", BitmapFormat.PNG)
    }
    val series = data.groupBy(_._1.network.p).zip(Seq(new Color(255, 0, 0), new Color(0, 255, 0), new Color(0, 0, 255))).map {
      case ((p, data), color) => (s"p=$p", Some(new Color(color.getRed, color.getGreen, color.getBlue, 5)), data.map(v => (v._3, Math.max(0.0, v._2))))
    }
    val chart = utils.Charts.scatterPlot("All", "Derrida", "Fitness",
      series,
      s => {
        s.setMarkerSize(8)
        s.setXAxisMax(2)
        s.setXAxisMin(0)
        s.setYAxisMax(200)
        s.setSeriesMarkers(Seq[Marker](new Circle(), new Circle(), new Circle()).toArray)
      })
    BitmapEncoder.saveBitmap(chart, s"${Analyzer.RESULT_FOLDER(args)}/derrida-scatterplot.png", BitmapFormat.PNG)
  }
}
