package main

import java.awt.Font

import com.github.plokhotnyuk.jsoniter_scala.core.{JsonValueCodec, readFromString}
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker
import model.RobotData
import model.config.Configuration
import org.knowm.xchart.BitmapEncoder.BitmapFormat
import org.knowm.xchart.style.BoxStyler.BoxplotCalCulationMethod
import org.knowm.xchart.style.Styler.LegendPosition
import org.knowm.xchart.{BitmapEncoder, BoxChartBuilder, SwingWrapper, XYChartBuilder}

import scala.reflect.ClassTag
import scala.util.{Failure, Success, Try}
import utils.Parallel._

object Analyzer extends App {

  implicit val arguments: Array[String] = args

  def RESULT_FOLDER(implicit args: Array[String]): String = Args.DATA_FOLDER(args) + "/results"

  implicit val srdCodec: JsonValueCodec[Seq[RobotData]] = JsonCodecMaker.make
  /** Load data of all experiments. */
  val rawData: Iterable[RobotData] = {
    var loaded = 0
    val result = Loader.OUTPUT_FILENAMES.parFlatmap(Args.PARALLELISM_DEGREE, { filename =>
      utils.File.read(filename).map { str =>
        println(s"Parsing $filename (${str.length} chars)")
        Try(readFromString[Seq[RobotData]](str)).getOrElse(Nil)
      } match {
        case Failure(exception) => Nil //println(s"Error while loading $filename: $exception"); Nil
        case Success(value) => loaded = loaded + 1; value
      }
    })
    println(s"Loaded $loaded experiments")
    result
  }

  /** Groups the raw data by configuration. */
  lazy val experimentsResults: Seq[(Configuration, Iterable[RobotData])] =
    rawData.groupBy(_.config.setControllersSeed(None).setSimulationSeed(None)).toList.sortBy(resultSorted)

  def resultSorted: ((Configuration, Iterable[RobotData])) => Int = {
    case (_, data) =>
      (data.map(_.fitnessCurve.last).sum / data.size * -1000).toInt
  }

  def showAveragedFitnessCharts(chartName: String, chartDescription: String, experimentsResults: Seq[(Configuration, Iterable[RobotData])], name: Configuration => String): Unit = {
    val chart = new XYChartBuilder().xAxisTitle("edits").yAxisTitle("average fitness")
      .title(s"Average fitness curve $chartDescription").width(1920).height(1080).build()
    chart.getStyler.setLegendFont(new Font("Computer Modern", Font.PLAIN, 18))
    chart.getStyler.setAxisTitleFont(new Font("Computer Modern", Font.PLAIN, 22))
    chart.getStyler.setChartTitleFont(new Font("Computer Modern", Font.PLAIN, 30))
    chart.getStyler.setAxisTickLabelsFont(new Font("Computer Modern", Font.PLAIN, 16))
    chart.getStyler.setLegendPosition(LegendPosition.InsideSE)
    chart.getStyler.setMarkerSize(0)
    experimentsResults.sortBy(resultSorted).foreach {
      case (config, values) =>
        val tests_count = values.head.fitnessCurve.size
        val totalFitnessCurve = values.map(_.fitnessCurve).foldLeft(0 until tests_count map (_ => 0.0)) {
          case (sum, curve) => sum.zip(curve).map(v => v._1 + v._2)
        }.map(_ / values.size)
        chart.addSeries(name(config), totalFitnessCurve.toArray)
    }
    BitmapEncoder.saveBitmapWithDPI(chart, RESULT_FOLDER + s"/$chartName-fitness-curve.png", BitmapFormat.PNG, 100)
    if (Args.SHOW_CHARTS) new SwingWrapper(chart).displayChart
  }

  def showBoxPlot(chartName: String, chartDescription: String, experimentsResults: Seq[(Configuration, Iterable[RobotData])], name: Configuration => String): Unit = {
    val chart = new BoxChartBuilder().xAxisTitle("variation").yAxisTitle("fitness")
      .title(s"Final fitness of each robot $chartDescription").width(1920).height(1080).build()
    chart.getStyler.setBoxplotCalCulationMethod(BoxplotCalCulationMethod.NP)
    chart.getStyler.setToolTipsEnabled(true)
    chart.getStyler.setPlotContentSize(0.98)
    chart.getStyler.setLegendFont(new Font("Computer Modern", Font.PLAIN, 28))
    chart.getStyler.setAxisTitleFont(new Font("Computer Modern", Font.PLAIN, 22))
    chart.getStyler.setChartTitleFont(new Font("Computer Modern", Font.PLAIN, 30))
    chart.getStyler.setAxisTickLabelsFont(new Font("Computer Modern", Font.PLAIN, 12))
    chart.getStyler.setXAxisLabelRotation(8)
    experimentsResults.sortBy(resultSorted).foreach {
      case (config, values) =>
        val result = values.map(_.fitnessCurve.last)
        chart.addSeries(name(config), result.toArray)
    }
    BitmapEncoder.saveBitmapWithDPI(chart, RESULT_FOLDER + s"/$chartName-boxplot.png", BitmapFormat.PNG, 100)
    if (Args.SHOW_CHARTS) new SwingWrapper(chart).displayChart
  }

  /**
   * Groups the results into groups, for every group a boxplot and a xy chart will be generated.
   * For each group the sub-results are grouped for series and the merged.
   */
  def makeCharts[G, S](experimentsResults: Seq[(Configuration, Iterable[RobotData])],
                       series: Configuration => S,
                       chartName: (Configuration, G) => String,
                       chartDescription: (Configuration, G) => String,
                       legend: (Configuration, G, S) => String,
                       groups: Configuration => G)(implicit classTag: ClassTag[G]): Unit = {
    experimentsResults.groupBy(v => groups(v._1)).foreach {
      case (group: G, groupResult: Seq[(Configuration, Iterable[RobotData])]) =>
        val results = groupResult.groupBy(v => series(v._1)).map {
          case (_, seq) => (seq.head._1, seq.flatMap(v => v._2))
        }.toSeq
        showAveragedFitnessCharts(chartName(groupResult.head._1, group), chartDescription(groupResult.head._1, group), results, config => s"${legend(config, group, series(config))}")
        showBoxPlot(chartName(groupResult.head._1, group), chartDescription(groupResult.head._1, group), results, config => s"${legend(config, group, series(config))}")
    }
  }

  /** Plots charts */
  if (Args.MAKE_CHARTS) {
    println("Plotting charts...")

    if (Settings.selectedExperiment.configVariation.size > 1) {
      Settings.selectedExperiment.configVariation.parForeach(Args.PARALLELISM_DEGREE, { v =>
        makeCharts[Unit, Any](experimentsResults,
          groups = _ => (),
          series = c => v.lens.get(c),
          chartName = (_, _) => s"${v.name}",
          chartDescription = (_, _) => s"(foreach ${v.name})",
          legend = (c, _, _) => s"${v.desc(c)}")
      })

      Settings.selectedExperiment.configVariation.filter(!_.collapse).parForeach(Args.PARALLELISM_DEGREE, { v =>
        makeCharts[Any, Any](experimentsResults,
          groups = c => v.lens.get(c),
          series = c => Settings.selectedExperiment.configVariation.filter(!_.collapse).map(v => v.lens.get(c)),
          chartName = (c, _) => s"${v.name}=${v.desc(c)}",
          chartDescription = (c, _) => s"(with ${v.name}=${v.desc(c)})",
          legend = (c, _, _) => Settings.selectedExperiment.configVariation.filter(!_.collapse).filter(_.name != v.name).map(v => s"${v.name}=${v.desc(c)}").mkString(","))
      })
    }

    //all series
    makeCharts[Any, Any](experimentsResults,
      groups = c => (),
      series = c => Settings.selectedExperiment.configVariation.filter(!_.collapse).map(v => v.lens.get(c)),
      chartName = (_, _) => s"overall",
      chartDescription = (_, _) => "",
      legend = (c, _, _) => Settings.selectedExperiment.configVariation.filter(!_.collapse).map(v => s"${v.name}=${v.desc(c)}").mkString(","))

  }

  /** Run a simulation where each robot has the best boolean network. */
  def runSimulationWithBestRobot(filter: Configuration => Boolean): Unit = {
    val bestRobot = rawData.filter(v => filter(v.config)).maxBy(_.fitness_values.sum)
    val bestConfig = bestRobot.config
    println("Best robot max fitness: " + bestRobot.fitnessCurve.last)
    val config = bestConfig.copy(simulation = bestConfig.simulation.copy(print_analytics = false), adaptation = bestConfig.adaptation.copy(epoch_length = 720000),
      network = bestConfig.network.copy(initial_schema = Some(bestRobot.best_network)))
    println(config)
    Experiments.runSimulation(config, visualization = true).foreach(println)
  }

  if (Args.RUN_BEST) {
    runSimulationWithBestRobot(config => true)
  }
}
