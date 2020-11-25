package model

import model.Types.Location
import model.config.Configuration

/**
 * Useful information related to a single robot contained in an experiment.
 *
 * @param robot_id       the robot id
 * @param config         the configuration of the experiment
 * @param fitness_values the fitness value at the end of each test of the experiment
 * @param location       the position and orientation of the robot over steps
 * @param best_network   the best boolean network (based on fitness) of this robot in this experiment
 */
case class RobotData(robot_id: String,
                     config: Configuration,
                     fitness_values: Seq[Double],
                     best_network: BooleanNetwork,
                     location: Seq[Location] = Nil) {
  def fitnessCurve: Seq[Double] = {
    fitness_values.scanLeft(0.0) {
      case (fitness, v) if v > fitness => v
      case (fitness, _) => fitness
    }.drop(1) //remove the initial 0.0
  }
}
