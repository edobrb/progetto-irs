import Loader.dataFormat
import model.RobotData
import model.config.Config
import org.knowm.xchart.BitmapEncoder.BitmapFormat
import org.knowm.xchart._
import org.knowm.xchart.style.BoxStyler.BoxplotCalCulationMethod
import play.api.libs.json.{JsError, JsSuccess, Json}
import utils.Parallel.Parallel

import scala.reflect.ClassTag
import scala.util.{Failure, Success}

object Analyzer extends App {

  implicit val arguments: Array[String] = args

  def RESULT_FOLDER(implicit args: Array[String]): String = Settings.DATA_FOLDER(args) + "/results"

  /** Load data of all experiments. */
  lazy val rawData: Iterable[RobotData] = Loader.OUTPUT_FILENAMES.parFlatmap(Settings.PARALLELISM_DEGREE, { filename =>
    utils.File.read(filename).map { str =>
      println(s"Parsing $filename (${str.length} chars)")
      Json.fromJson[Seq[RobotData]](Json.parse(str)) match {
        case JsSuccess(value, path) =>
          if(value.exists(_.fitness_values.size < 179)) {
            println(value)
          }
          value
        case JsError(errors) => println(s"Error while parsing $filename: $errors"); Nil
      }
    } match {
      case Failure(exception) => println(s"Error while loading $filename: $exception"); Nil
      case Success(value) => value
    }
  })

  /** Groups the raw data by configuration. */
  lazy val experimentsResults: Seq[(Config, Iterable[RobotData])] = rawData.groupBy(_.config).toList.sortBy(resultSorted)

  def resultSorted: ((Config, Iterable[RobotData])) => Int = {
    case (config, _) =>
      val bias = config.bn.options.bias
      val outputRewires = config.bn.max_output_rewires
      val selfLoops = config.bn.options.self_loops
      val nic = config.bn.options.network_inputs_count
      val fp = if (config.robot.feed_position) 1 else 0
      val soh = if (config.robot.stay_on_half) 1 else 0
      soh * 10000000 + fp * 1000000 + nic * 10000 + (bias * 1000).toInt + outputRewires * 10 + (if (selfLoops) 1 else 0)
  }

  def showAveragedFitnessCharts(chartName: String, experimentsResults: Seq[(Config, Iterable[RobotData])], name: Config => String): Unit = {
    val chart = new XYChartBuilder().xAxisTitle("tests").yAxisTitle("Average fitness")
      .title(s"Average fitness curve").width(1920).height(1080).build()
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

  def showBoxPlot(chartName: String, experimentsResults: Seq[(Config, Iterable[RobotData])], name: Config => String): Unit = {
    val chart = new BoxChartBuilder().xAxisTitle("steps").yAxisTitle("fitness")
      .title(s"Final fitness of each robot").width(1920).height(1080).build()
    chart.getStyler.setBoxplotCalCulationMethod(BoxplotCalCulationMethod.N_LESS_1_PLUS_1)
    chart.getStyler.setToolTipsEnabled(true)
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
  def makeCharts[G, S](experimentsResults: Seq[(Config, Iterable[RobotData])],
                       series: Config => S,
                       chartName: G => String,
                       legend: (Config, G, S) => String,
                       groups: Config => G)(implicit classTag: ClassTag[G]): Unit = {
    experimentsResults.groupBy(v => groups(v._1)).foreach {
      case (group: G, groupResult: Seq[(Config, Iterable[RobotData])]) =>
        val results = groupResult.groupBy(v => series(v._1)).map {
          case (_, seq) => (seq.head._1, seq.flatMap(v => v._2))
        }.toSeq
        showAveragedFitnessCharts(s"${chartName(group)}-fitness-curve", results, config => s"${legend(config, group, series(config))}")
        showBoxPlot(s"${chartName(group)}-boxplot", results, config => s"${legend(config, group, series(config))}")
    }
  }

  /** Plots charts */
  makeCharts[(Boolean, Boolean), Config](experimentsResults,
    groups = config => (config.robot.stay_on_half, config.robot.feed_position),
    series = c => c.copy(bn = c.bn.copy(options = c.bn.options.copy(self_loops = true))),
    chartName = {
      case (sh, fp) => s"${if (sh) "half-" else ""}${if (fp) "feed-" else ""}overall"
    },
    legend = {
      case (config, _, _) =>
        val bias = config.bn.options.bias
        val outputRewires = config.bn.max_output_rewires
        val nic = config.bn.options.network_inputs_count
        s"B=$bias,OR=$outputRewires,NIC=$nic"
    })
  makeCharts[Unit, Double](experimentsResults,
    groups = _ => (),
    series = _.bn.options.bias,
    chartName = _ => "bias",
    legend = (c, _, _) => s"bias=${c.bn.options.bias}")
  makeCharts[Unit, Int](experimentsResults,
    groups = _ => (),
    series = _.bn.options.network_inputs_count,
    chartName = _ => "nic",
    legend = (c, _, _) => s"input-node=${c.bn.options.network_inputs_count}")
  makeCharts[Unit, Int](experimentsResults,
    groups = _ => (),
    series = _.bn.max_output_rewires,
    chartName = _ => "or",
    legend = (c, _, _) => s"output-rewires=${c.bn.max_output_rewires}")
  makeCharts[Unit, Boolean](experimentsResults,
    groups = _ => (),
    series = _.bn.options.self_loops,
    chartName = _ => "self-loops",
    legend = (c, _, _) => if (c.bn.options.self_loops) "self loops" else "no self loops")
  makeCharts[Unit, (Boolean, Boolean)](experimentsResults,
    groups = _ => (),
    series = config => (config.robot.stay_on_half, config.robot.feed_position),
    chartName = _ => "variation",
    legend = {
      case (_, _, (false, _)) => "whole arena"
      case (_, _, (true, false)) => "half arena - no feed"
      case (_, _, (true, true)) => "half arena - feed"
    })


  /** Run a simulation where each robot has the best boolean network. */
  def runSimulationWithBestRobot(filter: Config => Boolean): Unit = {
    val bestRobot = rawData.filter(v => filter(v.config)).maxBy(_.fitnessCurve.last)
    val bestConfig = bestRobot.config
    println("Best robot in file: " + bestRobot.filename + "(" + bestRobot.fitnessCurve.last + ")")
    val config = bestConfig.copy(simulation = bestConfig.simulation.copy(network_test_steps = 720000, print_analytics = false),
      bn = bestConfig.bn.copy(initial = Some(bestRobot.bestBn)))
    println(config)
    Experiments.runSimulation(config, visualization = true).foreach(println)
  }

  runSimulationWithBestRobot(config => config.bn.options.bias == 0.79 && !config.robot.stay_on_half && !config.robot.feed_position)

  println("done")
}
