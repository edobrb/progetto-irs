import java.awt.Font

import Loader.dataFormat
import model.RobotData
import model.config.Configuration
import org.knowm.xchart.BitmapEncoder.BitmapFormat
import org.knowm.xchart._
import org.knowm.xchart.style.BoxStyler.BoxplotCalCulationMethod
import play.api.libs.json.{JsError, JsSuccess, Json}
import utils.Parallel.Parallel

import scala.reflect.ClassTag
import scala.util.{Failure, Success, Try}
import com.github.plokhotnyuk.jsoniter_scala.macros._
import com.github.plokhotnyuk.jsoniter_scala.core._

object Analyzer extends App {

  implicit val arguments: Array[String] = args

  def RESULT_FOLDER(implicit args: Array[String]): String = Args.DATA_FOLDER(args) + "/results"

  implicit val srdCodec: JsonValueCodec[Seq[RobotData]] = JsonCodecMaker.make
  /** Load data of all experiments. */
  lazy val rawData: Iterable[RobotData] = {
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

  def showAveragedFitnessCharts(chartName: String, experimentsResults: Seq[(Configuration, Iterable[RobotData])], name: Configuration => String): Unit = {
    val chart = new XYChartBuilder().xAxisTitle("edits").yAxisTitle("average fitness")
      .title(s"Average fitness curve").width(1920).height(1080).build()
    chart.getStyler.setLegendFont(new Font("Computer Modern", Font.PLAIN, 28))
    chart.getStyler.setAxisTitleFont(new Font("Computer Modern", Font.PLAIN, 22))
    chart.getStyler.setChartTitleFont(new Font("Computer Modern", Font.PLAIN, 30))
    chart.getStyler.setAxisTickLabelsFont(new Font("Computer Modern", Font.PLAIN, 16))
    experimentsResults.sortBy(resultSorted).foreach {
      case (config, values) =>
        val tests_count = values.head.fitnessCurve.size
        val totalFitnessCurve = values.map(_.fitnessCurve).foldLeft(0 until tests_count map (_ => 0.0)) {
          case (sum, curve) => sum.zip(curve).map(v => v._1 + v._2)
        }.map(_ / values.size)
        chart.addSeries(name(config), totalFitnessCurve.toArray)
    }
    BitmapEncoder.saveBitmapWithDPI(chart, RESULT_FOLDER + s"/$chartName.png", BitmapFormat.PNG, 100)
    if (Args.SHOW_CHARTS) new SwingWrapper(chart).displayChart
  }

  def showBoxPlot(chartName: String, experimentsResults: Seq[(Configuration, Iterable[RobotData])], name: Configuration => String): Unit = {
    val chart = new BoxChartBuilder().xAxisTitle("variation").yAxisTitle("fitness")
      .title(s"Final fitness of each robot").width(1920).height(1080).build()
    chart.getStyler.setBoxplotCalCulationMethod(BoxplotCalCulationMethod.N_LESS_1_PLUS_1)
    chart.getStyler.setToolTipsEnabled(true)
    chart.getStyler.setLegendFont(new Font("Computer Modern", Font.PLAIN, 28))
    chart.getStyler.setAxisTitleFont(new Font("Computer Modern", Font.PLAIN, 22))
    chart.getStyler.setChartTitleFont(new Font("Computer Modern", Font.PLAIN, 30))
    chart.getStyler.setAxisTickLabelsFont(new Font("Computer Modern", Font.PLAIN, 12))
    experimentsResults.sortBy(resultSorted).foreach {
      case (config, values) =>
        val result = values.map(_.fitnessCurve.last)
        chart.addSeries(name(config), result.toArray)
    }
    BitmapEncoder.saveBitmapWithDPI(chart, RESULT_FOLDER + s"/$chartName.png", BitmapFormat.PNG, 100)
    if (Args.SHOW_CHARTS) new SwingWrapper(chart).displayChart
  }

  /**
   * Groups the results into groups, for every group a boxplot and a xy chart will be generated.
   * For each group the sub-results are grouped for series and the merged.
   */
  def makeCharts[G, S](experimentsResults: Seq[(Configuration, Iterable[RobotData])],
                       series: Configuration => S,
                       chartName: (Configuration, G) => String,
                       legend: (Configuration, G, S) => String,
                       groups: Configuration => G)(implicit classTag: ClassTag[G]): Unit = {
    experimentsResults.groupBy(v => groups(v._1)).foreach {
      case (group: G, groupResult: Seq[(Configuration, Iterable[RobotData])]) =>
        val results = groupResult.groupBy(v => series(v._1)).map {
          case (_, seq) => (seq.head._1, seq.flatMap(v => v._2))
        }.toSeq
        showAveragedFitnessCharts(s"${chartName(groupResult.head._1, group)}-fitness-curve", results, config => s"${legend(config, group, series(config))}")
        showBoxPlot(s"${chartName(groupResult.head._1, group)}-boxplot", results, config => s"${legend(config, group, series(config))}")
    }
  }

  /** Plots charts */
  if (Args.MAKE_CHARTS) {
    println("Plotting charts...")
    Settings.variations.foreach { v =>
      makeCharts[Unit, Any](experimentsResults,
        groups = _ => (),
        series = c => v.lens.get(c),
        chartName = (_, _) => v.name,
        legend = (c, _, _) => s"${v.name}=${v.desc(c)}")
    }

    Settings.variations.filter(!_.collapse).foreach { v =>
      makeCharts[Any, Any](experimentsResults,
        groups = c => v.lens.get(c),
        series = c => Settings.variations.filter(!_.collapse).map(v => v.lens.get(c)),
        chartName = (c, _) => s"group-${v.name}-${v.desc(c)}",
        legend = (c, _, _) => Settings.variations.filter(_.name != v.name).map(v => s"${v.name}=${v.desc(c)}").mkString(","))
    }

  }

  /** Run a simulation where each robot has the best boolean network. */
  def runSimulationWithBestRobot(filter: Configuration => Boolean): Unit = {
    val bestRobot = rawData.filter(v => filter(v.config)).maxBy(_.fitnessCurve.last)
    val bestConfig = bestRobot.config
    println("Best robot in file: " + bestRobot.filename + "(" + bestRobot.fitnessCurve.last + ")")
    val config = bestConfig.copy(simulation = bestConfig.simulation.copy(print_analytics = false), adaptation = bestConfig.adaptation.copy(epoch_length = 720000),
      network = bestConfig.network.copy(initial_schema = Some(bestRobot.best_network)))
    println(config)
    Experiments.runSimulation(config, visualization = true).foreach(println)
  }

  if (Args.RUN_BEST) {
    runSimulationWithBestRobot(config => config.objective.half_region_variation.isEmpty)
  }
}
