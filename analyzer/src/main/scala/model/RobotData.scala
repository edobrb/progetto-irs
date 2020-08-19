package model

import model.config.Config

/**
 * Useful information related to a single robot contained in an experiment.
 *
 * @param filename       the filename of the experiment
 * @param config         the configuration of the experiment
 * @param robot_id       the robot id
 * @param fitness_values the fitness value at the end of each test of the experiment
 * @param bestBn         the best boolean network (based on fitness) of this robot in this experiment
 */
case class RobotData(filename: String, config: Config, robot_id: String, fitness_values: Seq[Double], bestBn: BooleanNetwork.Schema) {
  def fitnessCurve: Seq[Double] = {
    fitness_values.scanLeft(0.0) {
      case (fitness, v) if v > fitness => v
      case (fitness, _) => fitness
    }.drop(1) //remove the initial 0.0
  }
}