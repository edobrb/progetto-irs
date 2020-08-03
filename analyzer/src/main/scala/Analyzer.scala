import Loader.{Data, dataFormat}
import model.TestRun
import model.config.Config
import model.config.Config.JsonFormats._
import org.knowm.xchart.BitmapEncoder.BitmapFormat
import org.knowm.xchart.style.BoxStyler.BoxplotCalCulationMethod
import org.knowm.xchart.{SwingWrapper, _}
import play.api.libs.json.{Json, OFormat}

object Analyzer extends App {

  implicit def testRunFormat: OFormat[TestRun] = Json.format[TestRun]

  def RESULT_FOLDER = Experiments.DATA_FOLDER + "/results"

  def filenames: Iterable[String] = Loader.filenames.map(_ + ".json")

  lazy val rawData: Iterable[Data] = filenames.flatMap { filename =>
    utils.File.read(filename).map { str =>
      println("Parsing " + filename)
      Json.fromJson[Seq[Data]](Json.parse(str)).getOrElse(Nil) //.map(_.copy(bns = Nil))
    }.getOrElse(Nil)
  }

  def experimentsResults: Seq[(Config, Iterable[Data])] = rawData.groupBy(_.config).toList.sortBy {
    case (config, _) =>
      val bias = config.bn.options.bias
      val outputRewires = config.bn.max_output_rewires
      val selfLoops = config.bn.options.self_loops
      bias * 1000 + outputRewires * 50 + (if (selfLoops) 1 else 0)
  }

  def showAveragedFitnessCharts(): Unit =
    experimentsResults.foreach {
      case (config, values) =>
        val bias = config.bn.options.bias
        val outputRewires = config.bn.max_output_rewires
        val tests_count = values.head.fitness_curve.size
        val selfLoops = config.bn.options.self_loops
        val totalFitnessCurve = values.map(_.fitness_curve).foldLeft(0 until tests_count map (_ => 0.0)) {
          case (sum, curve) => sum.zip(curve).map(v => v._1 + v._2)
        }.map(_ / values.size)
        val chart: XYChart = QuickChart.getChart(s"Bias = $bias, OR = $outputRewires, SL = $selfLoops", "Step", "Fitness",
          "Avg fitness",
          totalFitnessCurve.indices.map(_.toDouble).toArray,
          totalFitnessCurve.toArray)

        BitmapEncoder.saveBitmap(chart, RESULT_FOLDER + s"/avg-fitness-curve-b$bias-or$outputRewires${if (selfLoops) "-sl" else ""}.png", BitmapFormat.PNG)
        new SwingWrapper(chart).displayChart
    }

  def showAveragedFitnessCharts2(): Unit = {
    val chart = new XYChartBuilder().xAxisTitle("steps").yAxisTitle("Average fitness")
      .title(s"Average fitness over steps").width(1920).height(1080).build()
    experimentsResults.foreach {
      case (config, values) =>
        val bias = config.bn.options.bias
        val outputRewires = config.bn.max_output_rewires
        val selfLoops = config.bn.options.self_loops
        val tests_count = values.head.fitness_curve.size
        val totalFitnessCurve = values.map(_.fitness_curve).foldLeft(0 until tests_count map (_ => 0.0)) {
          case (sum, curve) => sum.zip(curve).map(v => v._1 + v._2)
        }.map(_ / values.size)
        chart.addSeries(s"B=$bias,OR=$outputRewires${if (selfLoops) ",SL" else ""}", totalFitnessCurve.toArray)
    }
    BitmapEncoder.saveBitmapWithDPI(chart, RESULT_FOLDER + s"/asd.png", BitmapFormat.PNG, 300)
    new SwingWrapper(chart).displayChart
  }

  def showBoxPlot(): Unit = {
    experimentsResults.foreach {
      case (config, values) =>
        val bias = config.bn.options.bias
        val outputRewires = config.bn.max_output_rewires
        val selfLoops = config.bn.options.self_loops
        val result = values.map(_.fitness_curve.last)


        val chart: BoxChart = new BoxChartBuilder().title(s"Bias = $bias, OR = $outputRewires, SL = $selfLoops").build

        // Choose a calculation method
        chart.getStyler.setBoxplotCalCulationMethod(BoxplotCalCulationMethod.N_LESS_1_PLUS_1)
        chart.getStyler.setToolTipsEnabled(true)
        // Series
        chart.addSeries("Final fitness", result.toArray)

        BitmapEncoder.saveBitmap(chart, RESULT_FOLDER + s"/final-fitness-boxplot-b$bias-or$outputRewires${if (selfLoops) "-sl" else ""}.png", BitmapFormat.PNG)
        new SwingWrapper(chart).displayChart
    }

  }

  def showBoxPlot2(): Unit = {
    val chart = new BoxChartBuilder().xAxisTitle("steps").yAxisTitle("fitness")
      .title(s"Final fitness of each robot").width(1920).height(1080).build()
    chart.getStyler.setBoxplotCalCulationMethod(BoxplotCalCulationMethod.N_LESS_1_PLUS_1)
    chart.getStyler.setToolTipsEnabled(true)
    experimentsResults.foreach {
      case (config, values) =>
        val bias = config.bn.options.bias
        val outputRewires = config.bn.max_output_rewires
        val selfLoops = config.bn.options.self_loops
        val result = values.map(_.fitness_curve.last)
        chart.addSeries(s"B=$bias,OR=$outputRewires${if (selfLoops) ",SL" else ""}", result.toArray)
    }
    BitmapEncoder.saveBitmapWithDPI(chart, RESULT_FOLDER + s"/asd2.png", BitmapFormat.PNG, 300)
    new SwingWrapper(chart).displayChart
  }

  showAveragedFitnessCharts2()
  showBoxPlot2()

  val bestRobot = rawData.maxBy(_.fitness_curve.last)
  val bestConfig = bestRobot.config

  Experiments.runSimulation(bestConfig.copy(bn = bestConfig.bn.copy(initial = Some(bestRobot.bestBn))), visualization = true)
}
