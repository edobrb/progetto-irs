package main

import com.github.plokhotnyuk.jsoniter_scala.core.{JsonValueCodec, writeToString}
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker
import model.config.Configuration
import model.{RobotData, StepInfo}
import org.knowm.xchart.BitmapEncoder.BitmapFormat
import org.knowm.xchart.{BitmapEncoder, SwingWrapper, VectorGraphicsEncoder}
import org.knowm.xchart.VectorGraphicsEncoder.VectorGraphicsFormat
import play.api.libs.json._
import utils.Parallel.{Parallel, ParallelIterator}

import java.awt.Color
import scala.util.{Failure, Success}
import model.config.Configuration.JsonFormats._

import java.text.DecimalFormat

object Query extends App {
  implicit val arguments: Array[String] = args


  def robotsData(implicit args:Array[String]): Iterator[(RobotData, Seq[StepInfo])] = {
    implicit val siCodec: JsonValueCodec[StepInfo] = JsonCodecMaker.make
    Loader.FILENAMES(args).iterator.parMap(Args.PARALLELISM_DEGREE(args), {
      case (gzipFile, jsonFile) =>
        val tmpGzipFile = LoadBest.BEST_RAW_FOLDER(args) + "/" + gzipFile.split('/').last
        RobotData.loadsFromFile(jsonFile).flatMap(robotsData => {
          utils.File.readGzippedLinesAndMap(tmpGzipFile)(lines => {
            lines.map(l => Loader.toStepInfo(l)).collect {
              case Some(v) => v
            }.toIndexedSeq.groupBy(_.id)
          }).map(bests => robotsData.map(robotData => (robotData, bests(robotData.robot_id))))
        }) match {
          case Success(value) => println(s"$jsonFile loaded."); Success(value)
          case Failure(exception) => println(s"$jsonFile load failed. (${exception.getMessage})"); Failure(exception)
        }
    }).collect {
      case Success(value) => value
    }.flatten
  }

  case class StatesNumberResult(filename: String, statesCount: Int, fitness: Double)

  def queryStatesNumber(): Unit = {
    val RESULT_FOLDER = s"${Analyzer.RESULT_FOLDER}/${Args.CONFIGURATION}_states_count"
    val configs = Settings.configurations.map(v => v.filename -> v).toMap

    if (utils.Folder.create(RESULT_FOLDER).exists(_.isFailure)) {
      println("Cannot create result folders.")
      System.exit(-1)
    }

    implicit def resultFormat: OFormat[StatesNumberResult] = Json.format[StatesNumberResult]

    if(Args.LOAD_OUTPUT) {
      val results: IndexedSeq[StatesNumberResult] = robotsData.zipWithIndex.parMap(Args.PARALLELISM_DEGREE, {
        case ((robotsData, steps), i) =>

          if (robotsData.robot_id == "0") println(s"${i / 10} done.")

          val printOfOneEpoch = robotsData.config.adaptation.epoch_length * robotsData.config.simulation.ticks_per_seconds + 2
          val bestEpochInputs = steps.drop(1).dropRight(1).map(_.inputs.take(robotsData.config.objective.obstacle_avoidance.proximity_nodes))
          val inputsProbabilities = bestEpochInputs.groupBy(identity).map(v => v._2.size.toDouble / (printOfOneEpoch - 2))
          val entropy = utils.Entropy.shannon(inputsProbabilities)

          if(entropy > 1.4 && entropy < 2.5) {
            val bns = steps.drop(1).dropRight(1).scanLeft(robotsData.best_network)({
              case (bn, info) => bn.withInputs(info.inputs).step()
            }).groupBy(identity).map(v => (v._1, v._2.size))
            Some(StatesNumberResult(robotsData.config.filename, bns.size, robotsData.fitness_values.max))
          } else {
            None
          }

      }).collect {
        case Some(value) => value
      }.toIndexedSeq
      val json = Json.toJson(results).toString()
      utils.File.write(s"$RESULT_FOLDER/results.json", json)
    }

    val arenaVariation = Settings.selectedExperiment.configVariation.find(_.name == "arena").get
    val pVariation = Settings.selectedExperiment.configVariation.find(_.name == "p").get
    val adaptationVariation = Settings.selectedExperiment.configVariation.find(_.name == "adaptation").get

    val jsonStr = utils.File.read(s"$RESULT_FOLDER/results.json").get
    val json = Json.parse(jsonStr)
    val results: IndexedSeq[StatesNumberResult] = Json.fromJson[Seq[StatesNumberResult]](json).get.toIndexedSeq



    val formatter = new DecimalFormat("#")
    results.groupBy(_.filename).map(r => {
       (Settings.selectedExperiment.configVariation.map(v => v.desc(configs(r._1))), r._2.map(_.statesCount).sum / r._2.size.toDouble)
     }).groupBy(_._1(2)).foreach { r =>
       println(r._1)
       r._2.toSeq.map({
         case (p :: adattamento :: arena :: _, v) => (p, adattamento, v)
       }).groupBy(_._2).foreach {
         case (adattamento, res) =>
           val resP = res.groupBy(_._1)
           println(adattamento + " & " + formatter.format(resP("0.1").head._3) + " & " + formatter.format(resP("0.79").head._3) + " & " + formatter.format(resP("0.5").head._3))
       }
       println("")
     }


    results
      .filter(v => adaptationVariation.desc(configs(v.filename)) != "alterazione" /*&& configs(v.filename).network.p != 0.5*/)
      .groupBy(v => arenaVariation.desc(configs(v.filename))).foreach({
      case (arena, r) =>
        val series1 = r.groupBy({
          case StatesNumberResult(filename, _, _) => (pVariation.desc(configs(filename)), adaptationVariation.desc(configs(filename)))
        }).map {
          case ((p, adaptation), value) => (s"p:$p,$adaptation", value.map(_.statesCount.toDouble))
        }.toSeq.sortBy(_._2.sum)

        val chart = utils.Charts.boxplot("Arena " + arena, "Asd", "Numero di stati", series1, applyCustomStyle = v => {
          v.setPlotContentSize(0.90)
          v.setMarkerSize(5)
          v.setXAxisLabelRotation(15)
          v.setChartBackgroundColor(Color.white)
          v.setXAxisTitleVisible(false)
          v.setChartTitleVisible(true)

        },
          applyCustomBuild = _.width(600).height(500))
        BitmapEncoder.saveBitmap(chart, s"$RESULT_FOLDER/arena=$arena.png", BitmapFormat.PNG)
        VectorGraphicsEncoder.saveVectorGraphic(chart, s"$RESULT_FOLDER/arena=$arena.pdf", VectorGraphicsFormat.PDF)
    })
  }

  /*
  * Query best robot percentage
  * */
  def query1(): Unit = {
    val data: IndexedSeq[RobotData] = Analyzer.loadRobotsData(args).toIndexedSeq
    val arenaVariation = Settings.selectedExperiment(args).configVariation.find(_.name == "arena").get
    val eliteCount = 100
    val limits = data.groupBy(v => arenaVariation.desc(v.config)).map {
      case (arena, value) =>
        val bests = value.map(-_.fitness_values.max).sorted.take(eliteCount)
        (arena, -bests.last)
    }
    val results = data.groupBy(_.config).map {
      case (config, values) =>
        val limit = limits(arenaVariation.desc(config))
        val bestCount = values.count(_.fitness_values.max >= limit) / eliteCount.toDouble
        //val bestCount = values.filter(_.fitness_values.max > limit).map(_.fitness_values.max).sum
        val asd = values.map(_.fitness_values.count(_ >= limit)).sum / values.size.toDouble * 100
        (Settings.selectedExperiment(args).configVariation.map(_.desc(config)), (bestCount, asd))
    }
    results.foreach(println)
    limits.foreach(println)

    results.groupBy(_._1(2)).foreach { r =>
      println(r._1)
      r._2.toSeq.map({
        case (p :: adattamento :: arena :: _, (v, _)) => (p, adattamento, v)
      }).groupBy(_._2).foreach {
        case (adattamento, res) =>
          val resP = res.groupBy(_._1)
          println(adattamento + " & " + resP("0.1").head._3 + " & " + resP("0.79").head._3)
      }
      println("")
    }
  }

  def query3(): Unit = {
    val data: IndexedSeq[RobotData] = Analyzer.loadRobotsData(args).toIndexedSeq
    val arenaVariation = Settings.selectedExperiment(args).configVariation.find(_.name == "arena").get
    val eliteCount = 900
    val limits = data.groupBy(v => arenaVariation.desc(v.config)).map {
      case (arena, value) =>
        val bests = value.map(-_.fitness_values.max).sorted.take(eliteCount)
        (arena, -bests.last)
    }
    val results = data.groupBy(_.config).map {
      case (config, values) =>
        val limit = limits(arenaVariation.desc(config))
        val bestCount = values.count(_.fitness_values.max >= limit) / eliteCount.toDouble
        //val bestCount = values.filter(_.fitness_values.max > limit).map(_.fitness_values.max).sum
        val asd = values.map(_.fitness_values.count(_ >= limit)).sum / values.size.toDouble * 100
        (Settings.selectedExperiment(args).configVariation.map(_.desc(config)), (bestCount, asd))
    }
    results.foreach(println)
    limits.foreach(println)

    results.groupBy(_._1(2)).foreach { r =>
      println(r._1)
      r._2.toSeq.map({
        case (p :: adattamento :: arena :: _, (v, _)) => (p, adattamento, v)
      }).groupBy(_._2).foreach {
        case (adattamento, res) =>
          val resP = res.groupBy(_._1)
          println(adattamento + " & " + resP("0.1").head._3 + " & " + resP("0.79").head._3)
      }
      println("")
    }
  }


  queryStatesNumber()

}
