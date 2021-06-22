package main

import com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker
import model.config.Configuration
import model.{RobotData, StepInfo}
import play.api.libs.json.{Json, OFormat}
import utils.Parallel.Parallel
import model.config.Configuration.JsonFormats._
import org.knowm.xchart.{BitmapEncoder, VectorGraphicsEncoder}
import org.knowm.xchart.BitmapEncoder.BitmapFormat
import org.knowm.xchart.VectorGraphicsEncoder.VectorGraphicsFormat
import org.knowm.xchart.style.Styler.LegendPosition
import org.knowm.xchart.style.markers.{Circle, Marker}

import java.awt.Color
import scala.math.BigDecimal.double2bigDecimal
import scala.util.{Failure, Success}

object Entropy extends App {

  implicit val arguments: Array[String] = args

  case class Result(configuration: Configuration, fitness: Double, entropy: Double, robotId: String)

  implicit def resultFormat: OFormat[Result] = Json.format[Result]

  implicit val siCodec: JsonValueCodec[StepInfo] = JsonCodecMaker.make

  def ENTROPY_FOLDER(implicit args: Array[String]): String = s"${Analyzer.RESULT_FOLDER(args)}/${Args.CONFIGURATION(args)}_entropy"

  if (utils.Folder.create(ENTROPY_FOLDER + "/png", ENTROPY_FOLDER + "/csv").exists(_.isFailure)) {
    println("Cannot create entropy folder")
    System.exit(-1)
  }

  if (Args.LOAD_OUTPUT) {
    val results: Seq[Result] = Loader.FILENAMES_CONFIG(args).parMap(Args.PARALLELISM_DEGREE, {
      case (config, gzipFile, jsonFile) =>
        val tmpGzipFile = LoadBest.BEST_RAW_FOLDER + "/" + gzipFile.split('/').last
        RobotData.loadsFromFile(jsonFile).toOption.flatMap(robotsData => {
          val printOfOneEpoch = config.adaptation.epoch_length * config.simulation.ticks_per_seconds + 2
          utils.File.readGzippedLinesAndMap(tmpGzipFile)(lines => {
            lines.map(l => Loader.toStepInfo(l)).collect {
              case Some(v) => v
            }.toSeq.groupBy(_.id).map {
              case (robotId, steps) =>
                val bestEpochInputs = steps.drop(1).dropRight(1).map(_.inputs.take(config.objective.obstacle_avoidance.proximity_nodes))
                val inputsProbabilities = bestEpochInputs.groupBy(identity).map(v => v._2.size.toDouble / (printOfOneEpoch - 2))
                val entropy = utils.Entropy.shannon(inputsProbabilities)
                val fitness = steps.last.fitness
                println((gzipFile.split('-').last, fitness, entropy, robotId))
                Result(config, fitness, entropy, robotId)
            }
          }) match {
            case Failure(exception) => println(s"Error: ${exception.getMessage}"); None
            case Success(value) => Some(value)
          }
        }).getOrElse(Nil)
    }).flatten.toSeq


    val json = Json.toJson(results).toString()
    utils.File.write(s"$ENTROPY_FOLDER/results.json", json)
  }

  if (Args.MAKE_CHARTS) {
    println(s"Plotting charts at $ENTROPY_FOLDER")
    val jsonStr = utils.File.read(s"$ENTROPY_FOLDER/results.json").get
    val json = Json.parse(jsonStr)
    val results = Json.fromJson[Seq[Result]](json).get
      //.filter(_.configuration.network.p != 0.5)
    println(results.size)

    def variationName(configuration: Configuration): String = {
      val variations = Settings.selectedExperiment.configVariation
      variations.map(v => {
        if (v.legendName.nonEmpty) s"${v.legendName}: ${v.desc(configuration)}" else v.desc(configuration)
      }).mkString(",")
    }
    val resCsv = results.groupBy(_.configuration.setControllersSeed(None).setSimulationSeed(None)).map {
      case (configuration, value) =>
        val columnName = variationName(configuration)
        (columnName, value.map(_.entropy).toIndexedSeq, value.map(_.fitness).toIndexedSeq)
    }.toIndexedSeq
    val header1 = resCsv.map(_._1).mkString(";;")
    val header2 = resCsv.flatMap(_ => Seq("entropy", "fitness")).mkString(";")
    resCsv.indices.map(i => {
      resCsv(i)
    })
    val rows = resCsv.foldLeft(Seq[String]()) {
      case (Nil, (_, derridas, fitnesses)) => derridas.zip(fitnesses).map(v => v._1+";"+v._2)
      case (lines, (_, derridas, fitnesses)) => lines.zip(derridas.zip(fitnesses).map(v => v._1+";"+v._2)).map(v => v._1 + ";" + v._2)
    }
    utils.File.writeLines(s"$ENTROPY_FOLDER/results.csv", Seq(header1, header2) ++ rows)

    val alpha = 10
    val variations = Settings.selectedExperiment.configVariation.filter(v => !v.collapse && v.showDivided)
    variations.foreach(v => {
      results.groupBy(r => v.getVariation(r.configuration)).foreach {
        case (_, results: Seq[Result]) =>
          val config = results.head.configuration
          val title = s"${v.name}=${v.desc(config)}"
          val series = results.map(v => (v.entropy, Math.max(0.0, v.fitness)))
          val chart = utils.Charts.scatterPlot(title, "Entropia", "Fitness",
            Seq(("all", Some(new Color(255, 0, 0, alpha)), series)),
            s => {
              s.setMarkerSize(4)
              //s.setXAxisMax(6.0).setYAxisMax(90)
              s.setLegendVisible(false).setChartTitleVisible(false).setChartBackgroundColor(Color.WHITE)
            },
            _.width(800).height(600))
          BitmapEncoder.saveBitmap(chart, s"$ENTROPY_FOLDER/png/$title.png", BitmapFormat.PNG)
          val data = "entropy;fitness" +: series.map(v => "%.3f;%.3f".format(v._1, v._2))
          utils.File.writeLines(s"$ENTROPY_FOLDER/csv/$title.csv", data)
      }
    })
    Settings.selectedExperiment.configVariation.foreach(v => {
      val series = results.groupBy(r => v.getVariation(r.configuration)).values
        .map(r => (v.name + ": " + v.desc(r.head.configuration), r.map(v => (v.entropy, Math.max(0.0, v.fitness)))))
        .zip(Seq(new Color(255, 0, 0, alpha), new Color(0, 200, 0, alpha), new Color(0, 0, 255, alpha)))
        .map {
          case ((name, series), color) => (name, Some(color), series)
        }.toList.sortBy(_._1)
      val title = s"${v.name}"
      val chart = utils.Charts.scatterPlot(title, "Entropia", "Fitness", series,
        s => {
          s.setMarkerSize(4)
          s.setSeriesMarkers(Seq[Marker](new Circle(), new Circle(), new Circle()).toArray)
          //s.setXAxisMax(6.0).setYAxisMax(90)
          s.setChartTitleVisible(false).setChartBackgroundColor(Color.white)
          s.setLegendPosition(LegendPosition.InsideNE)
        },
        _.width(800).height(600))
      BitmapEncoder.saveBitmap(chart, s"$ENTROPY_FOLDER/png/$title.png", BitmapFormat.PNG)
    })

    //val hSerie = ("h", Option.apply(new Color(200,200,200)), (0.0 to 8 by 0.0025).map(e => (e.toDouble, 90 * utils.Entropy.h(e.toDouble, 1.7, 3, 0.75))).toSeq)
    val chart = utils.Charts.scatterPlot("All", "Entropia", "Fitness",
      Seq(("all", Some(new Color(255, 0, 0, alpha)), results.map(v => (v.entropy, Math.max(0.0, v.fitness))))),
      s => {
        s.setMarkerSize(4)
        //s.setXAxisMax(6.0).setYAxisMax(90)
        s.setChartTitleVisible(false).setChartBackgroundColor(Color.white).setLegendVisible(false)
        s.setSeriesMarkers(Seq[Marker](new Circle(), new Circle()).toArray)
      },
      _.width(800).height(600))
    BitmapEncoder.saveBitmap(chart, s"$ENTROPY_FOLDER/png/all.png", BitmapFormat.PNG)
  }
}
