import Loader.{Data, dataFormat}
import model.TestRun
import model.config.Config.JsonFormats._
import org.knowm.xchart.{QuickChart, XYChart}
import play.api.libs.json.{JsError, JsSuccess, Json, OFormat}

object Analyzer extends App {

  implicit def testRunFormat: OFormat[TestRun] = Json.format[TestRun]

  def showAveragedFitnessCharts() =
    utils.File.read(Experiments.DATA_FOLDER + "/result.json").map { str =>
      Json.fromJson[Seq[Data]](Json.parse(str)) match {
        case JsError(_) =>
        case JsSuccess(results, _) =>
          val asd = results.groupBy(_.config.bn.options.bias)
          asd.map {
            case (config, value: Seq[Data]) =>
              val tests_count = value.head.fitness_curve.size
              val totalFitnessCurve = value.map(_.fitness_curve).foldLeft(0 until tests_count map (_ => 0.0)) {
                case (sum, curve) => sum.zip(curve).map(v => v._1 + v._2)
              }.map(_ / value.size)

              println(totalFitnessCurve)


              val chart: XYChart = QuickChart.getChart("Bias = " + config.toString, "Step", "Fitness",
                "Averaged fitness over steps",
                totalFitnessCurve.indices.map(_.toDouble).toArray,
                totalFitnessCurve.toArray)
              import org.knowm.xchart.SwingWrapper
              new SwingWrapper(chart).displayChart
          }
      }
    }
  def showBestFitnessCharts() =
    utils.File.read(Experiments.DATA_FOLDER + "/result.json").map { str =>
      Json.fromJson[Seq[Data]](Json.parse(str)) match {
        case JsError(_) =>
        case JsSuccess(results, _) =>
          val asd = results.groupBy(_.config.bn.options.bias)
          asd.map {
            case (config, value: Seq[Data]) =>
              val tests_count = value.head.fitness_curve.size
              val totalFitnessCurve = value.maxBy(_.fitness_curve.last).fitness_curve



              val chart: XYChart = QuickChart.getChart("Bias = " + config.toString, "Step", "Fitness",
                "Averaged fitness over steps",
                totalFitnessCurve.indices.map(_.toDouble).toArray,
                totalFitnessCurve.toArray)
              import org.knowm.xchart.SwingWrapper
              new SwingWrapper(chart).displayChart
          }
      }
    }


  showAveragedFitnessCharts()
}
