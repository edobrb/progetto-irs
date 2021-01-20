package main

import com.github.plokhotnyuk.jsoniter_scala.core.{JsonValueCodec, writeToString}
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker
import main.FitnessHistogram.chart
import model.config.Configuration
import model.{RobotData, StepInfo}
import org.knowm.xchart.SwingWrapper
import utils.Parallel.{Parallel, ParallelIterator}

import java.awt.Color
import scala.collection.immutable.{AbstractMap, AbstractSeq, LinearSeq, SeqMap, SortedMap}
import scala.util.Success


object Query extends App {
  implicit val arguments: Array[String] = args
  implicit val siCodec: JsonValueCodec[StepInfo] = JsonCodecMaker.make

  def robotsData: Iterator[(RobotData, Seq[StepInfo])] = Loader.FILENAMES(args).iterator.parMap(Args.PARALLELISM_DEGREE, {
    case (gzipFile, jsonFile) =>
      val tmpGzipFile = LoadBest.BEST_RAW_FOLDER + "/" + gzipFile.split('/').last
      RobotData.loadsFromFile(jsonFile).flatMap(robotsData => {
        utils.File.readGzippedLinesAndMap(tmpGzipFile)(lines => {
          lines.map(l => Loader.toStepInfo(l)).collect {
            case Some(v) => v
          }.toIndexedSeq.groupBy(_.id)
        }).map(bests => robotsData.map(robotData => (robotData, bests(robotData.robot_id))))
      }) match {
        case Success(value) => println(s"$jsonFile loaded."); Success(value)
        case failure => println(s"$jsonFile load failed."); failure
      }
  }).collect {
    case Success(value) => value
  }.flatten

  //val data: IndexedSeq[RobotData] = Analyzer.loadRobotsData(args).toIndexedSeq

  query2()

  def query2():Unit = {
    val results: Map[Configuration, IndexedSeq[(Configuration, Int, Double)]] = robotsData.zipWithIndex.parMap(Args.PARALLELISM_DEGREE, {
      case ((robotsData, steps), i) =>
        val bns = steps.drop(1).dropRight(1).scanLeft(robotsData.best_network)({
          case (bn, info) => bn.withInputs(info.inputs).step()
        }).groupBy(identity).map(v => (v._1, v._2.size))
        if(robotsData.robot_id == "0") println(s"${i/10} done.")
        (robotsData.config.setControllersSeed(None).setSimulationSeed(None), bns.size, robotsData.fitness_values.max)
    }).toIndexedSeq.groupBy(_._1)

    results.map(r => {
      (Settings.selectedExperiment(args).configVariation.map(v => v.desc(r._1)), r._2.map(_._2).sum / r._2.size.toDouble)
    }).groupBy(_._1(2)).foreach { r =>
      println(r._1)
      r._2.toSeq.map({
        case (p :: adattamento :: arena :: _, v) => (p, adattamento, v)
      }).groupBy(_._2).foreach {
        case (adattamento, res) =>
          val resP = res.groupBy(_._1)
          println(adattamento + " & " + resP("0.1").head._3 + " & " + resP("0.79").head._3 + " & " + resP("0.5").head._3)
      }
      println("")
    }

    val arenaVariation = Settings.selectedExperiment(args).configVariation.find(_.name == "arena").get
    results.toSeq.flatMap(_._2).groupBy(v => arenaVariation.desc(v._1)).foreach(r => {
      val pVariation = Settings.selectedExperiment(args).configVariation.find(_.name == "p").get
      val adaptationVariation = Settings.selectedExperiment(args).configVariation.find(_.name == "adaptation").get


      val series2 = Seq(("some", Some(new Color(255, 0, 0, 20)), r._2.map(v => (v._2.toDouble, v._3))))

      val series1 = r._2.groupBy({
        case (config, _, _) => (pVariation.desc(config), adaptationVariation.desc(config))
      }).map {
        case ((p, adaptation), value) => (s"p: $p $adaptation", value.map(_._2.toDouble))
      }.toSeq.sortBy(_._2.sum)


      val chart1 = utils.Charts.boxplot("Arena "+ r._1, "Numero di stati", "Fitness", series1,  applyCustomStyle = v => {
        v.setPlotContentSize(0.90)
        v.setMarkerSize(5)
        v.setXAxisLabelRotation(20)
        v.setChartBackgroundColor(Color.white)
        v.setChartTitleVisible(true)
      },
        applyCustomBuild = _.width(600).height(600))
      val chart2 = utils.Charts.scatterPlot("Arena "+ r._1, "Variante", "Numero di stati", series2,  applyCustomStyle = v => {
        v.setPlotContentSize(0.90)
        v.setMarkerSize(5)
        v.setXAxisLabelRotation(20)
        v.setChartBackgroundColor(Color.white)
        v.setChartTitleVisible(true)
        v.setLegendVisible(false)
      },
        applyCustomBuild = _.width(600).height(600))
      new SwingWrapper(chart2).displayChart
    })
  }
  /*
  * Query best robot percentage
  * */
  def query1(data: Seq[RobotData]):Unit = {
    val arenaVariation = Settings.selectedExperiment(args).configVariation.find(_.name == "arena").get
    val eliteCount = 100
    val limits = data.groupBy(v => arenaVariation.desc(v.config)).map {
      case (arena, value) =>
        val bests = value.map(-_.fitness_values.max).sorted.take(eliteCount)
        (arena , -bests.last)
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
          println(adattamento + " & " + resP("0.1").head._3 + " & " + resP("0.79").head._3 + " & " + resP("0.5").head._3)
      }
      println("")
    }
  }


}
