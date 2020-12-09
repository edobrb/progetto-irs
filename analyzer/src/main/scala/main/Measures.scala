package main

import experiments.E9
import model.{BooleanNetwork, RobotData}
import org.knowm.xchart.BitmapEncoder.BitmapFormat
import org.knowm.xchart.style.markers.{Circle, Marker}
import org.knowm.xchart.{BitmapEncoder, SwingWrapper, XYChartBuilder}
import utils.Parallel.Parallel

import java.awt.Font

object Measures extends App {

  implicit val arguments: Array[String] = args

  val robotsData = Analyzer.loadRobotsData(args)

  robotsData.groupBy(v => v.config.copy(network = v.config.network.copy(p = 0)).setControllersSeed(None).setSimulationSeed(None)).foreach {
    case (config, data) =>

      val series = data.groupBy(_.config.network.p).map {
        case (p, data) =>
          val series = data.parMap(8, {
            case RobotData(robot_id, config, fitness_values, best_network, locations) =>
              val bn = best_network
              val derrida = (0 until 1000).map(i => {
                bn.invertRandomStates(1).next(1).statesHammingDistance(bn.next(1))
              })
              println(fitness_values.max + " " + derrida.sum.toDouble / derrida.size)
              (fitness_values.max, derrida.sum.toDouble / derrida.size)

            /* val map = bn.next(1000).statesMap(steps = 1000, perturbation = _.invertRandomInputs(1))
             val count = map.maxBy(_._2)._1.statesHammingDistance(bn)
             println(fitness_values.max + " " + count)
             (fitness_values.max, count)*/
          })
          (s"p=${p}", series)
      }

      val mutation = config.adaptation.network_mutation.max_connection_rewires > 0
      val rewire = config.adaptation.network_io_mutation.max_input_rewires > 0
      val title = s"${config.simulation.argos.split('/').last}-rewire=$rewire-mutation=$mutation"
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
      chart.getStyler.setSeriesMarkers(Seq[Marker](new Circle(), new Circle(), new Circle()).toArray)
      series.foreach {
        case (name, s) => chart.addSeries(name, s.map(_._2.toDouble).toArray, s.map(_._1).toArray)
      }
      //new SwingWrapper(chart).displayChart
      BitmapEncoder.saveBitmap(chart, s"/home/edo/Desktop/$title-derrida-scatterplot.png", BitmapFormat.PNG)
  }





  /*val statesMap = bn.statesMap(steps = 10000, perturbation = _.invertRandomInputs(1))

  statesMap.filter(_._2 > 9).toSeq.sortBy(_._2)foreach {
    case (value, i) => println(value.prettyStatesString+" ["+i+"]")
  }
  println(statesMap.count(_._2 > 9) +"/"+ statesMap.size + "=" + statesMap.count(_._2 > 1).toDouble / statesMap.size)*/


}
