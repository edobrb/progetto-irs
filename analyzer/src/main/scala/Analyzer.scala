import Loader.{Data, dataFormat}
import model.TestRun
import model.config.Config.JsonFormats._
import org.knowm.xchart.BitmapEncoder.BitmapFormat
import org.knowm.xchart.style.BoxStyler.BoxplotCalCulationMethod
import org.knowm.xchart._
import org.knowm.xchart.internal.chartpart.Chart
import play.api.libs.json.{Json, OFormat}

object Analyzer extends App {

  implicit def testRunFormat: OFormat[TestRun] = Json.format[TestRun]

  def RESULT_FOLDER = Experiments.DATA_FOLDER + "/results"

  def filenames: Iterable[String] = Loader.filenames.map(_ + ".json")

  lazy val experimentsResults: Iterable[Data] = filenames.flatMap { filename =>
    utils.File.read(filename).map { str =>
      println("Parsing " + filename)
      Json.fromJson[Seq[Data]](Json.parse(str)).getOrElse(Nil).map(_.copy(bns = Nil))
    }.getOrElse(Nil)
  }

  def showAveragedFitnessCharts(): Unit =
    experimentsResults.groupBy(_.config).foreach {
      case (config, values) =>
        val bias = config.bn.options.bias
        val outputRewires = config.bn.max_output_rewires
        val tests_count = values.head.fitness_curve.size
        val totalFitnessCurve = values.map(_.fitness_curve).foldLeft(0 until tests_count map (_ => 0.0)) {
          case (sum, curve) => sum.zip(curve).map(v => v._1 + v._2)
        }.map(_ / values.size)
        val chart: XYChart = QuickChart.getChart(s"Bias = $bias, OR = $outputRewires", "Step", "Fitness",
          "Avg fitness",
          totalFitnessCurve.indices.map(_.toDouble).toArray,
          totalFitnessCurve.toArray)
        import org.knowm.xchart.SwingWrapper

        BitmapEncoder.saveBitmap(chart, RESULT_FOLDER + s"/avg-fitness-curve-b$bias-or$outputRewires.png", BitmapFormat.PNG)
        new SwingWrapper(chart).displayChart
    }

  def showBoxPlot(): Unit = {
    experimentsResults.groupBy(_.config).foreach {
      case (config, values) =>
        val bias = config.bn.options.bias
        val outputRewires = config.bn.max_output_rewires
        val result = values.map(_.fitness_curve.last)


        val chart: BoxChart = new BoxChartBuilder().title(s"Bias = $bias, OR = $outputRewires").build

        // Choose a calculation method
        chart.getStyler.setBoxplotCalCulationMethod(BoxplotCalCulationMethod.N_LESS_1_PLUS_1)
        chart.getStyler.setToolTipsEnabled(true)
        // Series
        chart.addSeries("Final fitness", result.toArray)

        BitmapEncoder.saveBitmap(chart, RESULT_FOLDER + s"/final-fitness-boxplot-b$bias-or$outputRewires.png", BitmapFormat.PNG)
        new SwingWrapper(chart).displayChart
    }

  }

  showAveragedFitnessCharts()
  showBoxPlot()
}
