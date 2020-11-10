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

  def RESULT_FOLDER(implicit args: Array[String]): String = Settings.DATA_FOLDER(args) + "/results"

  implicit val srdCodec: JsonValueCodec[Seq[RobotData]] = JsonCodecMaker.make
  /** Load data of all experiments. */
  lazy val rawData: Iterable[RobotData] = Loader.OUTPUT_FILENAMES.parFlatmap(Settings.PARALLELISM_DEGREE, { filename =>
    utils.File.read(filename).map { str =>
      println(s"Parsing $filename (${str.length} chars)")
      Try(readFromString[Seq[RobotData]](str)).getOrElse(Nil)
    } match {
      case Failure(exception) => println(s"Error while loading $filename: $exception"); Nil
      case Success(value) => value
    }
  })

  /** Groups the raw data by configuration. */
  lazy val experimentsResults: Seq[(Configuration, Iterable[RobotData])] =
    rawData.groupBy(_.config.setControllersSeed(None).setSimulationSeed(None)).toList.sortBy(resultSorted)

  def resultSorted: ((Configuration, Iterable[RobotData])) => Int = {
    case (config, _) =>
      val bias = config.network.p
      val outputRewires = config.adaptation.network_io_mutation.max_output_rewires
      val selfLoops = config.network.self_loops
      val nic = config.objective.obstacle_avoidance.proximity_nodes
      val fp = if (config.objective.half_region_variation.exists(_.region_nodes > 0)) 1 else 0
      val soh = if (config.objective.half_region_variation.isDefined) 1 else 0
      soh * 10000000 + fp * 1000000 + nic * 10000 + (bias * 1000).toInt + outputRewires * 10 + (if (selfLoops) 1 else 0)
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
  }

  /**
   * Groups the results into groups, for every group a boxplot and a xy chart will be generated.
   * For each group the sub-results are grouped for series and the merged.
   */
  def makeCharts[G, S](experimentsResults: Seq[(Configuration, Iterable[RobotData])],
                       series: Configuration => S,
                       chartName: G => String,
                       legend: (Configuration, G, S) => String,
                       groups: Configuration => G)(implicit classTag: ClassTag[G]): Unit = {
    experimentsResults.groupBy(v => groups(v._1)).foreach {
      case (group: G, groupResult: Seq[(Configuration, Iterable[RobotData])]) =>
        val results = groupResult.groupBy(v => series(v._1)).map {
          case (_, seq) => (seq.head._1, seq.flatMap(v => v._2))
        }.toSeq
        showAveragedFitnessCharts(s"${chartName(group)}-fitness-curve", results, config => s"${legend(config, group, series(config))}")
        showBoxPlot(s"${chartName(group)}-boxplot", results, config => s"${legend(config, group, series(config))}")
    }
  }

  /** Plots charts */
  makeCharts[(Boolean, Boolean), Configuration](experimentsResults,
    groups = config => (config.objective.half_region_variation.isDefined, config.objective.half_region_variation.exists(_.region_nodes > 0)),
    series = c => c.copy(network = c.network.copy(self_loops = true)),
    chartName = {
      case (sh, fp) => s"${if (sh) "half-" else ""}${if (fp) "feed-" else ""}overall"
    },
    legend = {
      case (config, _, _) =>
        val bias = config.network.p
        val outputRewires = config.adaptation.network_io_mutation.max_output_rewires
        val nic = config.objective.obstacle_avoidance.proximity_nodes
        s"B=$bias,OR=$outputRewires,NIC=$nic"
    })
  makeCharts[Unit, Double](experimentsResults,
    groups = _ => (),
    series = _.network.p,
    chartName = _ => "bias",
    legend = (c, _, _) => s"bias=${c.network.p}")
  makeCharts[Unit, Int](experimentsResults,
    groups = _ => (),
    series = _.objective.obstacle_avoidance.proximity_nodes,
    chartName = _ => "nic",
    legend = (c, _, _) => s"input-node=${c.objective.obstacle_avoidance.proximity_nodes}")
  makeCharts[Unit, Int](experimentsResults,
    groups = _ => (),
    series = _.adaptation.network_io_mutation.max_output_rewires,
    chartName = _ => "or",
    legend = (c, _, _) => s"output-rewires=${c.adaptation.network_io_mutation.max_output_rewires}")
  makeCharts[Unit, Boolean](experimentsResults,
    groups = _ => (),
    series = _.network.self_loops,
    chartName = _ => "self-loops",
    legend = (c, _, _) => if (c.network.self_loops) "self loops" else "no self loops")
  makeCharts[Unit, (Boolean, Boolean)](experimentsResults,
    groups = _ => (),
    series = config => (config.objective.half_region_variation.isDefined, config.objective.half_region_variation.exists(_.region_nodes > 0)),
    chartName = _ => "variation",
    legend = {
      case (_, _, (false, _)) => "whole arena"
      case (_, _, (true, false)) => "half arena - no feed"
      case (_, _, (true, true)) => "half arena - feed"
    })


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

  runSimulationWithBestRobot(config => true)

  println("done")
}
