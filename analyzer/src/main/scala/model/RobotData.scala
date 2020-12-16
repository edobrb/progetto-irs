package model

import com.github.plokhotnyuk.jsoniter_scala.core.{JsonValueCodec, readFromString}
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker
import model.Types.Location
import model.config.Configuration

import scala.util.Try

/**
 * Useful information related to a single robot contained in an experiment.
 *
 * @param robot_id       the robot id
 * @param config         the configuration of the experiment
 * @param fitness_values the fitness value at the end of each test of the experiment
 * @param locations      the position and orientation of the robot over steps
 * @param best_network   the best boolean network (based on fitness) of this robot in this experiment
 */
case class RobotData(robot_id: String,
                     config: Configuration,
                     fitness_values: Seq[Double],
                     best_network: BooleanNetwork,
                     locations: Seq[Location] = Nil) {
  def fitnessMaxCurve: Seq[Double] =
    fitness_values.map(v => if (v < 0) 0 else v).scanLeft(0.0) {
      case (fitness, v) if v > fitness => v
      case (fitness, _) => fitness
    }.drop(1) //remove the initial 0.0

  def fitnessSumCurve: Seq[Double] =
    fitness_values.map(v => if (v < 0) 0 else v).scanLeft(0.0) {
      case (fitness, v) => fitness + v
    }.drop(1) //remove the initial 0.0
}

object RobotData {
  def loadsFromFile(filename: String): Try[Seq[RobotData]] = {
    implicit val srdCodec: JsonValueCodec[Seq[RobotData]] = JsonCodecMaker.make
    utils.File.read(filename).map(str => Try(readFromString[Seq[RobotData]](str))).flatten
  }
}
