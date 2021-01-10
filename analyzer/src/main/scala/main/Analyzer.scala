package main


import com.github.plokhotnyuk.jsoniter_scala.core.{JsonValueCodec, readFromString}
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker
import model.RobotData
import model.config.Configuration
import org.knowm.xchart.BitmapEncoder.BitmapFormat
import org.knowm.xchart.VectorGraphicsEncoder.VectorGraphicsFormat
import org.knowm.xchart.style.Styler.LegendPosition
import org.knowm.xchart.{BitmapEncoder, SwingWrapper, VectorGraphicsEncoder}

import scala.reflect.ClassTag
import scala.util.{Failure, Success, Try}
import utils.Parallel._

import java.awt.Color


object Analyzer extends App {

  implicit val arguments: Array[String] = args

  def RESULT_FOLDER(implicit args: Array[String]): String = Args.DATA_FOLDER(args) + "/results"

  def CHARTS_FOLDER(implicit args: Array[String]): String = s"${RESULT_FOLDER(args)}/${Args.CONFIGURATION(args)}_charts"

  def CHARTS_DATA_FOLDER(implicit args: Array[String]): String = s"${RESULT_FOLDER(args)}/${Args.CONFIGURATION(args)}_charts_data"

  val configs = Settings.configurations.map(v => v.filename -> v).toMap

  /** Load all data of the specified experiment. */
  def loadRobotsData(implicit args: Array[String]): Iterable[RobotData] = {
    implicit val srdCodec: JsonValueCodec[Seq[RobotData]] = JsonCodecMaker.make

    println("Loading files...")
    val result = Loader.OUTPUT_FILENAMES(args).parMap(Args.PARALLELISM_DEGREE(args), { filename =>
      utils.File.read(filename).map({ str =>
        println(s"Parsing $filename (${str.length} chars)")
        Try(readFromString[Seq[RobotData]](str)) match {
          case Failure(_) => Left(filename)
          case Success(value) => Right(value.map(v => v.copy(config = configs(v.config.filename))))
        }
      })
    }).map {
      case Success(value) => Success(value)
      case Failure(ex) => println(ex); Failure(ex)
    }.collect {
      case Success(v) => v
    }

    println("Broken files: " + result.collect {
      case Left(filename) => filename
    }.map(f => f.split('/').last).mkString(" | "))

    val robotsData = result.flatMap {
      case Right(data) => data
      case Left(_) => Nil
    }

    println(s"Loaded ${robotsData.size} robots data")
    robotsData
  }

  val robotsData: Iterable[RobotData] = loadRobotsData

  /** Groups the raw data by configuration. */
  lazy val experimentsResults: Seq[(Configuration, Iterable[RobotData])] =
    robotsData.groupBy(_.config).toList.sortBy(resultSorted)

  def resultSorted: ((Configuration, Iterable[RobotData])) => Int = {
    case (_, data) =>
      (data.map(_.fitnessMaxCurve.last).sum / data.size * -1000).toInt
  }

  def averageFitnessSeries(experimentsResults: Seq[(Configuration, Iterable[RobotData])]): Seq[(Configuration, Iterable[(Double, Double)])] =
    experimentsResults.sortBy(resultSorted).map {
      case (config, values) =>
        val tests_count = values.head.fitnessMaxCurve.size
        val totalFitnessCurve = values.map(_.fitnessMaxCurve).foldLeft(0 until tests_count map (_ => 0.0)) {
          case (sum, curve) => sum.zip(curve).map(v => v._1 + v._2)
        }.map(_ / values.size).zipWithIndex.map(v => (v._2.toDouble, v._1))
        (config, totalFitnessCurve)
    }

  def finalFitnessSeries(experimentsResults: Seq[(Configuration, Iterable[RobotData])]): Seq[(Configuration, Iterable[Double])] =
    experimentsResults.sortBy(resultSorted).map {
      case (config, values) => (config, values.map(_.fitnessMaxCurve.last))
    }

  def saveAveragedFitnessCharts(chartName: String, chartDescription: String, series: Seq[(String, Option[Color], Iterable[(Double, Double)])]): Unit = {
    val chart = utils.Charts.linePlot(s"Average fitness curve $chartDescription", "edits", "average fitness",
      series, v => {
        v.setLegendPosition(LegendPosition.InsideSE);
        v.setMarkerSize(0)
        v.setChartTitleVisible(false)
        v.setChartBackgroundColor(Color.white)
      }, _.width(1920).height(1080))
    def chartFileName(format:String):String = s"$CHARTS_FOLDER/$format/$chartName-fitness-curve.$format"
    BitmapEncoder.saveBitmap(chart, chartFileName("png"), BitmapFormat.PNG)
    VectorGraphicsEncoder.saveVectorGraphic(chart, chartFileName("pdf"), VectorGraphicsFormat.PDF)
    VectorGraphicsEncoder.saveVectorGraphic(chart, chartFileName("svg"), VectorGraphicsFormat.SVG)
    println(s"saved ${chartFileName("png")}")
    if (Args.SHOW_CHARTS) new SwingWrapper(chart).displayChart
  }

  def saveAverageFitnessData(chartName: String, series: Seq[(String, Option[Color], Iterable[(Double, Double)])]) = {
    val epochCount = series.minBy(_._3.size)._3.size
    val rows = series.foldLeft((1 to epochCount).map(_.toString)) {
      case (lines, (_, _, series)) => lines.zip(series.map(_._2.toString)).map(v => v._1 + ";" + v._2)
    }
    val header = (Seq("epoch") ++ series.map {
      case (legend, _, _) => legend
    }).mkString(";")
    utils.File.writeLines(s"$CHARTS_DATA_FOLDER/$chartName-fitness-curve.csv", header +: rows)
  }

  def saveBoxPlot(chartName: String, chartDescription: String, series: Seq[(String, Iterable[Double])]): Unit = {
    val chart = utils.Charts.boxplot(s"Final fitness of each robot $chartDescription", "variation", "fitness",
      series,
      applyCustomStyle = _.setChartBackgroundColor(Color.white).setChartTitleVisible(false),
      applyCustomBuild = _.width(1920).height(1080))
    def chartFileName(format:String):String = s"$CHARTS_FOLDER/$format/$chartName-boxplot.$format"
    BitmapEncoder.saveBitmap(chart, chartFileName("png"), BitmapFormat.PNG)
    VectorGraphicsEncoder.saveVectorGraphic(chart, chartFileName("pdf"), VectorGraphicsFormat.PDF)
    VectorGraphicsEncoder.saveVectorGraphic(chart, chartFileName("svg"), VectorGraphicsFormat.SVG)
    println(s"saved ${chartFileName("png")}")
    if (Args.SHOW_CHARTS) new SwingWrapper(chart).displayChart
  }

  def saveBoxPlotData(chartName: String, series: Seq[(String, Iterable[Double])]) = {
    val rows = series.foldLeft(Seq[String]()) {
      case (Nil, (_, series)) => series.map(_.toString).toSeq
      case (lines, (_, series)) => lines.zip(series.map(_.toString)).map(v => v._1 + ";" + v._2)
    }
    val header = series.map {
      case (legend, _) => legend
    }.mkString(";")
    utils.File.writeLines(s"$CHARTS_DATA_FOLDER/$chartName-boxplot.csv", header +: rows)
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
        val chartNameV = chartName(groupResult.head._1, group)
        val chartDescriptionV = chartDescription(groupResult.head._1, group)
        val legendGenerator: Configuration => String = config => s"${legend(config, group, series(config))}"
        val averageFitness = averageFitnessSeries(results).map {
          case (config, series) => (legendGenerator(config), None, series)
        }
        val finalFitness = finalFitnessSeries(results).map {
          case (config, value) => (legendGenerator(config), value)
        }
        saveAveragedFitnessCharts(chartNameV, chartDescriptionV, averageFitness)
        saveAverageFitnessData(chartNameV, averageFitness)
        saveBoxPlot(chartNameV, chartDescriptionV, finalFitness)
        saveBoxPlotData(chartNameV, finalFitness)
    }
  }

  /** Plots charts */
  if (Args.MAKE_CHARTS) {
    println("Plotting charts...")
    if (utils.Folder.create(CHARTS_FOLDER, CHARTS_DATA_FOLDER, s"$CHARTS_FOLDER/png", s"$CHARTS_FOLDER/svg", s"$CHARTS_FOLDER/pdf").exists(_.isFailure)) {
      println("Cannot create result folders.")
      System.exit(-1)
    }

    if (Settings.selectedExperiment.configVariation.size > 1) {
      Settings.selectedExperiment.configVariation.parForeach(Args.PARALLELISM_DEGREE, { v =>
        makeCharts[Unit, Any](experimentsResults,
          groups = _ => (),
          series = c => v.getVariation(c),
          chartName = (_, _) => s"${v.name}",
          chartDescription = (_, _) => s"(foreach ${v.name})",
          legend = (c, _, _) => s"${v.desc(c)}")
      })

      val variations = Settings.selectedExperiment.configVariation.filter(v => !v.collapse && v.showDivided)
      makeCharts[Any, Any](experimentsResults,
        groups = c => variations.map(_.getVariation(c)),
        series = c => c,
        chartName = (c, _) => variations.map(v => s"${v.name}=${v.desc(c)}").mkString(","),
        chartDescription = (c, _) => variations.map(v => s"(with ${v.name}=${v.desc(c)})").mkString(","),
        legend = (c, _, _) => Settings.selectedExperiment.configVariation.filter(!_.collapse)
          .filter(v => !variations.map(_.name).contains(v.name)).map(v => s"${v.name}=${v.desc(c)}").mkString(","))
    }

    //all series
    /*makeCharts[Any, Any](experimentsResults,
      groups = c => (),
      series = c => Settings.selectedExperiment.configVariation.filter(!_.collapse).map(v => v.getVariation(c)),
      chartName = (_, _) => s"overall",
      chartDescription = (_, _) => "",
      legend = (c, _, _) => Settings.selectedExperiment.configVariation.filter(!_.collapse).map(v => s"${v.name}=${v.desc(c)}").mkString(","))*/
  }

  /** Run a simulation where each robot has the best boolean network. */
  def runSimulationWithBestRobot(filter: Configuration => Boolean, selector: RobotData => Double): Unit = {
    val bestRobot = robotsData.filter(v => filter(v.config)).maxBy(selector)
    val bestConfig = bestRobot.config
    println("Best robot max fitness: " + bestRobot.fitnessMaxCurve.last)
    val config = bestConfig.copy(simulation = bestConfig.simulation.copy(print_analytics = false), adaptation = bestConfig.adaptation.copy(epoch_length = 720000),
      network = bestConfig.network.copy(initial_schema = Some(bestRobot.best_network), initial_state = Some(bestRobot.best_network.states)))
    println(config)
    Experiments.runSimulation(config, visualization = true).foreach(println)
  }

  if (Args.RUN_BEST) {
    runSimulationWithBestRobot(config => true, _.fitnessMaxCurve.sum)
  }
}
