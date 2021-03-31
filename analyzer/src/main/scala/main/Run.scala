package main

import model.{RobotData, StepInfo}
import model.config.Configuration


object Run extends App {

  implicit val arguments: Array[String] = args
  val data: Iterator[(RobotData, Seq[StepInfo])] = Query.robotsData

  def filter1(robot: RobotData, steps: Seq[StepInfo]): Boolean = {
    val printOfOneEpoch = robot.config.adaptation.epoch_length * robot.config.simulation.ticks_per_seconds + 2
    val epochInputs = steps.drop(1).dropRight(1).map(_.inputs.take(robot.config.objective.obstacle_avoidance.proximity_nodes))
    val inputsProbabilities = epochInputs.groupBy(identity).map(v => v._2.size.toDouble / (printOfOneEpoch - 2))
    val entropy = utils.Entropy.shannon(inputsProbabilities)
    val fitness = steps.last.fitness
    1.72 < entropy && entropy < 1.77 && 80 < fitness && fitness < 48
  }

  data.filter {
    case (data, value) => filter1(data, value)
  }.map(_._1).foreach { robot =>
      val config: Configuration = robot.config
      val finalConfig = config.copy(
        simulation = config.simulation.copy(print_analytics = false),
        adaptation = config.adaptation.copy(epoch_length = 720000),
        network = config.network.copy(
          initial_schema = Some(robot.best_network),
          initial_state = Some(robot.best_network.states)))
      Experiments.runSimulation(finalConfig, visualization = true).foreach(println)
  }


}
