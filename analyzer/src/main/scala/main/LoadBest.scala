package main

import com.github.plokhotnyuk.jsoniter_scala.core.{JsonValueCodec, writeToString}
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker
import main.Entropy.args
import model.Types.RobotId
import model.{RobotData, StepInfo}
import utils.Parallel.Parallel

object LoadBest extends App {
  implicit val arguments: Array[String] = args
  implicit val siCodec: JsonValueCodec[StepInfo] = JsonCodecMaker.make

  def BEST_RAW_FOLDER(implicit args: Array[String]): String = Analyzer.RESULT_FOLDER(args) + "/best_raw"

  if (utils.Folder.create(BEST_RAW_FOLDER).exists(_.isFailure)) {
    println("Cannot create best_raw folder")
    System.exit(-1)
  }

  Loader.FILENAMES(args).parForeach(Args.PARALLELISM_DEGREE, {
    case (gzipFile, jsonFile) =>
      val bestRawFile = BEST_RAW_FOLDER + "/" + gzipFile.split('/').last
      if (utils.File.exists(bestRawFile)) {
        println(s"Skipping $bestRawFile")
      } else {
        RobotData.loadsFromFile(jsonFile).toOption.map(robotsData => {
          //id -> (config, (fitness,epoch),(toDrop,toTake))
          val maxes = robotsData.map(data => {
            val printOfOneEpoch = data.config.adaptation.epoch_length * data.config.simulation.ticks_per_seconds + 2
            val (bestFitness, bestEpoch) = data.fitness_values.zipWithIndex.maxBy(_._1)
            val toDrop = printOfOneEpoch * bestEpoch
            (data.robot_id, (data.config, (bestFitness, bestEpoch), (toDrop, printOfOneEpoch)))
          }).toMap

          val (lines, source) = utils.File.readGzippedLines(gzipFile).get

          val config = robotsData.head.config
          val printOfOneEpoch = config.adaptation.epoch_length * config.simulation.ticks_per_seconds + 2
          val allSteps = lines.map(l => Loader.toStepInfo(l)).collect {
            case Some(v) => v
          }
          val steps: Iterator[StepInfo] =
            if (config.other.contains("target_entropy")) {
              val targetEntropy = config.other("target_entropy").toDouble
              allSteps.toIndexedSeq.groupBy(_.id).flatMap {
                case (robotId, robotsSteps) => robotsSteps.grouped(printOfOneEpoch).minBy(steps => {
                  val epochInputs = steps.drop(1).dropRight(1).map(_.inputs.take(config.objective.obstacle_avoidance.proximity_nodes))
                  val inputsProbabilities = epochInputs.groupBy(identity).map(v => v._2.size.toDouble / (printOfOneEpoch - 2))
                  val entropy = utils.Entropy.shannon(inputsProbabilities)
                  (targetEntropy - entropy) * (targetEntropy - entropy)
                })
              }.iterator
            } else {
              allSteps.filter(v => {
                val (_, _, (toDrop, toTake)) = maxes(v.id)
                toDrop <= v.step && v.step < (toDrop + toTake)
              })
            }

          utils.File.writeGzippedLines(bestRawFile, steps.map(step => writeToString(step)))
          source.close()
          println(s"Done $bestRawFile")
        }).getOrElse(Nil)
      }
  })
}
