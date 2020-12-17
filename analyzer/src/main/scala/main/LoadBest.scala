package main

import com.github.plokhotnyuk.jsoniter_scala.core.{JsonValueCodec, writeToString}
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker
import main.Entropy.args
import model.{RobotData, StepInfo}
import utils.Parallel.Parallel

object LoadBest extends App {
  implicit val arguments: Array[String] = args
  implicit val siCodec: JsonValueCodec[StepInfo] = JsonCodecMaker.make

  Loader.FILENAMES(args).parForeach(Args.PARALLELISM_DEGREE, {
    case (gzipFile, jsonFile) =>
      RobotData.loadsFromFile(jsonFile).toOption.map(robotsData => {

        //id -> (config, (fitness,epoch),(toDrop,toTake))
        val maxes = robotsData.map(data => {
          val printOfOneEpoch = data.config.adaptation.epoch_length * data.config.simulation.ticks_per_seconds + 2
          val (bestFitness, bestEpoch) = data.fitness_values.zipWithIndex.maxBy(_._1)
          val toDrop = printOfOneEpoch * bestEpoch
          (data.robot_id, (data.config, (bestFitness, bestEpoch), (toDrop, printOfOneEpoch)))
        }).toMap

        val (lines, source) = utils.File.readGzippedLines(gzipFile).get

        val steps: Iterator[StepInfo] = lines.map(l => Loader.toStepInfo(l)).collect {
          case Some(v) => v
        }.filter(v => {
          val (_, _, (toDrop, toTake)) = maxes(v.id)
          toDrop <= v.step && v.step < (toDrop + toTake)
        })


        utils.File.writeGzippedLines(Analyzer.RESULT_FOLDER(args) + "/tmp/" + gzipFile.split('/').last, steps.map(step => writeToString(step)))

        source.close()

        println(s"Done $gzipFile")

      }).getOrElse(Nil)
  })
}
