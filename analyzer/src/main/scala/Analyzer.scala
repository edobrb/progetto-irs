import Loader.{Data, dataFormat}
import model.TestRun
import model.config.Config.JsonFormats._
import org.knowm.xchart.{QuickChart, XYChart}
import play.api.libs.json.{Json, OFormat}

object Analyzer extends App {

  implicit def testRunFormat: OFormat[TestRun] = Json.format[TestRun]

  val filenames = Seq("0.1", "0.5", "0.79").flatMap(f => (1 to 30).map(Experiments.DATA_FOLDER + "/" + f + "-" + _ + ".json"))

  val experimentsResults: Seq[Data] = filenames.flatMap { filename =>
    utils.File.read(filename).map { str =>
      Json.fromJson[Seq[Data]](Json.parse(str)).getOrElse(Nil)
    }.getOrElse(Nil)
  }

  def showAveragedFitnessCharts() =
    experimentsResults.groupBy(_.config.bn.options.bias).map {
      case (config, value: Seq[Data]) =>
        val tests_count = value.head.fitness_curve.size
        val totalFitnessCurve = value.map(_.fitness_curve).foldLeft(0 until tests_count map (_ => 0.0)) {
          case (sum, curve) => sum.zip(curve).map(v => v._1 + v._2)
        }.map(_ / value.size)
        val chart: XYChart = QuickChart.getChart("Bias = " + config.toString, "Step", "Fitness",
          "Averaged fitness over steps",
          totalFitnessCurve.indices.map(_.toDouble).toArray,
          totalFitnessCurve.toArray)
        import org.knowm.xchart.SwingWrapper
        new SwingWrapper(chart).displayChart
    }

  showAveragedFitnessCharts()
}
