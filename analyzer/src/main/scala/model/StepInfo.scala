package model

import model.Types.{Fitness, Location, RobotId}

/**
 * The basic information that every robot of the simulation generates every step.
 *
 * @param step            step number of the simulation
 * @param id              id of the robot
 * @param boolean_network optionally, the boolean network schema of the robot
 * @param inputs          the inputs that perturbed the boolean network this step
 * @param fitness         the actual fitness computed by the robot
 * @param location        the position and orientation of the robot (option needed for retro compatibility)
 */
case class StepInfo(step: Int,
                    id: RobotId,
                    boolean_network: Option[BooleanNetwork],
                    inputs: Seq[Boolean],
                    fitness: Fitness,
                    location: Location)
