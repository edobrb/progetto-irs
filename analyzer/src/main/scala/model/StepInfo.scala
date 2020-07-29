package model

import model.Types.{Fitness, ProximityValues}

/**
 * The basic information that every robot of the simulation generates every step.
 *
 * @param step step number of the simulation
 * @param id id of the robot
 * @param boolean_network optionally, the boolean network schema of the robot
 * @param states the state of the boolean network
 * @param fitness the actual fitness computed by the robot
 * @param proximity the proximity data of the robot's sensors
 */
case class StepInfo(step: Int,
                    id: String,
                    boolean_network: Option[BooleanNetwork.Schema],
                    states: BooleanNetwork.State,
                    fitness: Fitness,
                    proximity: ProximityValues)
