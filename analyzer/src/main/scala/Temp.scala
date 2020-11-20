import Experiments.args
import Loader.{extractTests2, load, toStepInfo}
import com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker
import model.{Epoch, RobotData, StepInfo}
import model.config.Configuration
import play.api.libs.json.Json
import utils.Benchmark

import scala.concurrent.duration.FiniteDuration
import scala.util.{Failure, Success, Try}

object Temp extends App {


  implicit val arguments: Array[String] = args
  Settings.experiments.filter(_._1 == "6651f8c5ffbc10205fa03e68fb4dc3ac81293d3319403924834da28b4f343a6a-1").toList.sortBy(_._3).foreach { e =>
    val filename = Args.DATA_FOLDER + "/" + e._1
    val output_filename = filename + ".gzip"
    implicit val siCodec: JsonValueCodec[StepInfo] = JsonCodecMaker.make
    utils.File.readGzippedLinesAndMap(output_filename) {
      content: Iterator[String] =>
        val asd = Loader.extractTests(content.map(Loader.toStepInfo).collect{case Some(v) => v}.filter(_.id=="0"))
        val epochs = asd("0")
        val finalBn = epochs(2).states.foldLeft(epochs(2).bn)({
          case (bn, (input, _, _)) if input.nonEmpty =>
            val bn2 = bn(input)
            println(bn2.states.toString.replace(" ",""))
            bn2
          case (bn, _) =>
            println(bn.states.toString.replace(" ",""))
            bn
        })
        println(epochs(3).bn.states == finalBn.states)
    }
  }


  /*val asd = (1 to 3).flatMap(i => Settings.experiments(s"config=$i,from=1,to=1000".split(',')))

  asd.groupBy(v => (v._2.simulation.controllers_random_seed.get, v._2.simulation.simulation_random_seed.get)).filter(_._2.size > 1).foreach { v =>
    println(v._1+" -> "+v._2.map(x => (x._3, x._2.filename)))
  }*/

}
