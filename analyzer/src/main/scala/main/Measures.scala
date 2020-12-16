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

object Measures extends App {

  implicit val arguments: Array[String] = args

  if (Args.LOAD_OUTPUT) {
    println("Loading files...")
    val robotsData: MapView[Configuration, Seq[(Double, Double)]] = Loader.OUTPUT_FILENAMES(args).parMap(Args.PARALLELISM_DEGREE(args), { filename =>
      RobotData.loadsFromFile(filename).map(_.map {
        case RobotData(robot_id, config, fitness_values, best_network, locations) =>
          val bn = best_network
          val derrida = (0 until 1000).map(i => {
            bn.invertRandomStates(1).next(1).statesHammingDistance(bn.next(1))
          })
          println(fitness_values.max + " " + derrida.sum.toDouble / derrida.size)
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
    robotsData.flatMap({
      case (configuration, value) => value.map(v => (configuration, v._1, v._2))
    }).groupBy(v => v._1.copy(network = v._1.network.copy(p = 0)).setControllersSeed(None).setSimulationSeed(None)).foreach {
      case (config, data) =>

        val series = data.groupBy(_._1.network.p).map {
          case (p, data) => (s"p=$p", data.map(v => (v._2, v._3)))
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
        val chart = new XYChartBuilder().xAxisTitle("Derrida").yAxisTitle("Fitness")
          .title(title).width(1600).height(900).build()
        import org.knowm.xchart.XYSeries.XYSeriesRenderStyle
        chart.getStyler.setDefaultSeriesRenderStyle(XYSeriesRenderStyle.Scatter)
        chart.getStyler.setLegendFont(new Font("Computer Modern", Font.PLAIN, 18))
        chart.getStyler.setAxisTitleFont(new Font("Computer Modern", Font.PLAIN, 22))
        chart.getStyler.setChartTitleFont(new Font("Computer Modern", Font.PLAIN, 30))
        chart.getStyler.setAxisTickLabelsFont(new Font("Computer Modern", Font.PLAIN, 16))
        //chart.getStyler.setLegendPosition(LegendPosition.InsideSE)
        chart.getStyler.setMarkerSize(8)
        chart.getStyler.setXAxisMax(2)
        chart.getStyler.setXAxisMin(0)
        chart.getStyler.setYAxisMax(200)

        chart.getStyler.setSeriesMarkers(Seq[Marker](new Circle(), new Circle(), new Circle()).toArray)
        series.zip(Seq(new Color(255, 0, 0), new Color(0, 255, 0), new Color(0, 0, 255))).foreach {
          case ((name, s), color) =>
            val chartSeries = chart.addSeries(name, s.map(_._2).toArray, s.map(_._1).map(Math.abs).toArray)
            chartSeries.setMarkerColor(new Color(color.getRed, color.getGreen, color.getBlue, 30))
        }
        //new SwingWrapper(chart).displayChart
        BitmapEncoder.saveBitmap(chart, s"${Analyzer.RESULT_FOLDER(args)}/$title-derrida-scatterplot.png", BitmapFormat.PNG)
    }
  }

  /*val statesMap = bn.statesMap(steps = 10000, perturbation = _.invertRandomInputs(1))

  statesMap.filter(_._2 > 9).toSeq.sortBy(_._2)foreach {
    case (value, i) => println(value.prettyStatesString+" ["+i+"]")
  }
  println(statesMap.count(_._2 > 9) +"/"+ statesMap.size + "=" + statesMap.count(_._2 > 1).toDouble / statesMap.size)*/


}
