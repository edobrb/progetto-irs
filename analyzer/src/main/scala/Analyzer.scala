import Loader.dataFormat
import model.RobotData
import model.config.Config
import org.knowm.xchart.BitmapEncoder.BitmapFormat
import org.knowm.xchart.style.BoxStyler.BoxplotCalCulationMethod
import org.knowm.xchart.{SwingWrapper, _}
import play.api.libs.json.{JsError, JsSuccess, Json}

import scala.util.{Failure, Success}

object Analyzer extends App {

  def RESULT_FOLDER: String = Experiments.DATA_FOLDER + "/results"

  /** Load data of all experiments. **/
  lazy val rawData: Iterable[RobotData] = Loader.OUTPUT_FILENAMES.flatMap { filename =>
    utils.File.read(filename).map { str =>
      println(s"Parsing $filename (${str.length} chars)")
      Json.fromJson[Seq[RobotData]](Json.parse(str)) match {
        case JsSuccess(value, path) => value
        case JsError(errors) => println(s"Error while parsing $filename: $errors"); Nil
      } //.map(_.copy(bns = Nil))
    } match {
      case Failure(exception) => println(s"Error while loading $filename: $exception"); Nil
      case Success(value) => value
    }
  }

  /** Groups the raw data by configuration. **/
  lazy val experimentsResults: Seq[(Config, Iterable[RobotData])] = rawData.groupBy(_.config).toList.sortBy {
    case (config, _) =>
      val bias = config.bn.options.bias
      val outputRewires = config.bn.max_output_rewires
      val selfLoops = config.bn.options.self_loops
      val nic = config.bn.options.network_inputs_count
      val fp = if (config.robot.feed_position) 1 else 0
      val soh = if (config.robot.stay_on_half) 1 else 0
      soh * 10000000 + fp * 1000000 + nic * 10000 + bias * 1000 + outputRewires * 10 + (if (selfLoops) 1 else 0)
  }

  def nameSeries(config: Config): String = {
    val bias = config.bn.options.bias
    val outputRewires = config.bn.max_output_rewires
    val selfLoops = config.bn.options.self_loops
    val nic = config.bn.options.network_inputs_count
    val fp = config.robot.feed_position
    val soh = config.robot.stay_on_half
    s"B=$bias,OR=$outputRewires${if (selfLoops) ",SL" else ""},NIC=$nic${if (soh) ",H" else ""}${if (fp) ",FP" else ""}"
  }

  def showAveragedFitnessCharts(chartName:String, experimentsResults: Seq[(Config, Iterable[RobotData])]): Unit = {
    val chart = new XYChartBuilder().xAxisTitle("tests").yAxisTitle("Average fitness")
      .title(s"Average fitness curve").width(1920).height(1080).build()
    experimentsResults.foreach {
      case (config, values) =>
        val tests_count = values.head.fitnessCurve.size
        val totalFitnessCurve = values.map(_.fitnessCurve).foldLeft(0 until tests_count map (_ => 0.0)) {
          case (sum, curve) => sum.zip(curve).map(v => v._1 + v._2)
        }.map(_ / values.size)
        chart.addSeries(nameSeries(config), totalFitnessCurve.toArray)
    }
    BitmapEncoder.saveBitmapWithDPI(chart, RESULT_FOLDER + s"/$chartName.png", BitmapFormat.PNG, 100)
    new SwingWrapper(chart).displayChart
  }

  def showBoxPlot(chartName:String,experimentsResults: Seq[(Config, Iterable[RobotData])]): Unit = {
    val chart = new BoxChartBuilder().xAxisTitle("steps").yAxisTitle("fitness")
      .title(s"Final fitness of each robot").width(2500).height(1080).build()
    chart.getStyler.setBoxplotCalCulationMethod(BoxplotCalCulationMethod.N_LESS_1_PLUS_1)
    chart.getStyler.setToolTipsEnabled(true)
    experimentsResults.foreach {
      case (config, values) =>
        val result = values.map(_.fitnessCurve.last)
        chart.addSeries(nameSeries(config), result.toArray)
    }
    BitmapEncoder.saveBitmapWithDPI(chart, RESULT_FOLDER + s"/$chartName.png", BitmapFormat.PNG, 100)
    new SwingWrapper(chart).displayChart
  }

  /** Plots charts. **/
  experimentsResults.groupBy {
    case (config, _) => (config.robot.stay_on_half, config.robot.feed_position, config.bn.options.network_inputs_count)
  } foreach {
    case ((sh, fp, nic), results) =>
      showAveragedFitnessCharts(s"${if(sh) "half-" else ""}${if(fp) "feed-" else ""}nic=${nic}-avg-fitness-curve", results)
      showBoxPlot(s"${if(sh) "half-" else ""}${if(fp) "feed-" else ""}nic=${nic}-fitness-boxplot", results)
  }

  val comparison = experimentsResults.filter(v => !v._1.robot.stay_on_half && !v._1.bn.options.self_loops && v._1.bn.max_output_rewires == 1)
  showAveragedFitnessCharts(s"comparison-avg-fitness-curve", comparison)
  showBoxPlot(s"comparison-fitness-boxplot", comparison)

  /** Run a simulation where each robot has the best boolean network. **/
  val bestRobot = rawData.maxBy(_.fitnessCurve.last)
  val bestConfig = bestRobot.config
  println("Best robot in file: " + bestRobot.filename + "(" + bestRobot.fitnessCurve.last + ")")

  val config = bestConfig.copy(simulation = bestConfig.simulation.copy(network_test_steps = 7200, print_analytics = false),
    bn = bestConfig.bn.copy(initial = Some(bestRobot.bestBn)))
  println(config)
  Experiments.runSimulation(config, visualization = true).foreach(println)
}
