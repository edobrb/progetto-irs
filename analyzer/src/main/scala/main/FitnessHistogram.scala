package main

import model.RobotData
import org.knowm.xchart.SwingWrapper
import utils.Parallel.Parallel
import scala.util.Success
import org.knowm.xchart.CategoryChartBuilder


object FitnessHistogram extends App {

  implicit val arguments: Array[String] = args

  println("Loading files...")
  val robotsData: Iterable[Double] = Loader.OUTPUT_FILENAMES(args).parMap(Args.PARALLELISM_DEGREE(args), { filename =>
    RobotData.loadsFromFile(filename).map(robotsData => {
      robotsData.map {
        case RobotData(robot_id, config, fitness_values, best_network, locations) =>
          (config, fitness_values.max)
      }
    })
  }).collect({
    case Success(v) => v
  }).flatten
    //.filter(_._1.objective.half_region_variation.isDefined)
    //.filter(_._1.simulation.argos == "experiments/parametrized.argos")
    .map(_._2)

  println("Loaded " + robotsData.size)

  val resolution: Double = 1
  val data = robotsData.map(v => Math.abs((v * resolution).toInt / resolution))
    .groupBy(v => v)
    .map(v => (v._1, v._2.size))
    //.filter(_._2 > 100)
    .toSeq.sortBy(_._1)


  val chart = new CategoryChartBuilder().width(1600).height(900).title("Fitness Histogram").xAxisTitle("Fitness")
    .yAxisTitle("Times").build()

  chart.getStyler.setLegendVisible(false)
  chart.getStyler.setPlotGridVerticalLinesVisible(false)
  chart.getStyler.setXAxisMaxLabelCount(20)
  chart.addSeries("test 1", data.map(_._1.toDouble).toArray, data.map(_._2.toDouble).toArray)
  new SwingWrapper(chart).displayChart
}
