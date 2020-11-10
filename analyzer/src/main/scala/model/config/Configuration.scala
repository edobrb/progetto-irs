package model.config

import model.config.Configuration._
import play.api.libs.json.{Json, OFormat}

case class Configuration(simulation: Simulation = Simulation(),
                         adaptation: Adaptation = Adaptation(),
                         network: Network = Network(),
                         objective: Objective = Objective()) {
  def toJson: String = {
    import JsonFormats._
    Json.toJson(this).toString()
  }

  def expectedLines: Int = {
    val argosInfoPrints = 0
    val initialConfigPrints = 1
    val stepPrints = simulation.experiment_length * simulation.ticks_per_seconds * simulation.robot_count
    val initialBnConfigPrints = stepPrints / adaptation.epoch_length
    stepPrints + argosInfoPrints + initialConfigPrints + initialBnConfigPrints
  }

  def filename: String =
    utils.Hash.sha256(setSimulationSeed(None).setControllersSeed(None).toString)

  def setSimulationSeed(seed: Option[Int]): Configuration =
    copy(simulation = simulation.copy(simulation_random_seed = seed))

  def setControllersSeed(seed: Option[Int]): Configuration =
    copy(simulation = simulation.copy(controllers_random_seed = seed))
}

object Configuration {

  case class Simulation(ticks_per_seconds: Int = 10,
                        experiment_length: Int = 7200,
                        robot_count: Int = 10,
                        print_analytics: Boolean = true,
                        controllers_random_seed: Option[Int] = None,
                        simulation_random_seed: Option[Int] = None)

  case class Adaptation(epoch_length: Int = 400,
                        network_mutation: NetworkMutation = NetworkMutation(), //selection, crossover?
                        network_io_mutation: NetworkIOMutation = NetworkIOMutation())

  case class NetworkMutation(max_connection_rewires: Int = 0,
                             connection_rewire_probability: Double = 1,
                             max_function_bit_flips: Int = 0,
                             function_bit_flips_probability: Double = 1,
                             keep_p_balance: Boolean = false)

  case class NetworkIOMutation(max_input_rewires: Int = 2,
                               input_rewire_probability: Double = 1,
                               max_output_rewires: Int = 1,
                               output_rewire_probability: Double = 1,
                               allow_io_node_overlap: Boolean = false)

  case class Network(n: Int = 100,
                     k: Int = 3,
                     p: Double = 0.79,
                     self_loops: Boolean = true,
                     io: NetworkIO = NetworkIO(),
                     initial_schema: Option[model.BooleanNetwork.Schema] = None,
                     initial_state: Option[model.BooleanNetwork.State] = None)

  case class NetworkIO(override_output_nodes: Boolean = true,
                       override_outputs_p: Double = 0.5,
                       allow_io_node_overlap: Boolean = false)

  case class Objective(forwarding: Forwarding = Forwarding(),
                       obstacle_avoidance: ObstacleAvoidance = ObstacleAvoidance(),
                       half_region_variation: Option[HalfRegionVariation] = None)

  case class Forwarding(max_wheel_speed: Double = 50,
                        wheels_nodes: Int = 2)

  case class ObstacleAvoidance(proximity_threshold: Double = 0.1,
                               proximity_nodes: Int = 8)

  case class HalfRegionVariation(region_nodes: Int = 1,
                                 penalty_factor: Double = 0,
                                 reset_region_every_epoch: Boolean = false)


  def fromJson(json: String): Configuration = {
    import JsonFormats._
    Json.fromJson[Configuration](Json.parse(json)).get
  }

  object JsonFormats {
    implicit def f0: OFormat[model.BooleanNetwork.Schema] = Json.format[model.BooleanNetwork.Schema]

    implicit def f1: OFormat[Configuration.HalfRegionVariation] = Json.format[Configuration.HalfRegionVariation]

    implicit def f2: OFormat[Configuration.ObstacleAvoidance] = Json.format[Configuration.ObstacleAvoidance]

    implicit def f3: OFormat[Configuration.Forwarding] = Json.format[Configuration.Forwarding]

    implicit def f4: OFormat[Configuration.Objective] = Json.format[Configuration.Objective]

    implicit def f5: OFormat[Configuration.NetworkIO] = Json.format[Configuration.NetworkIO]

    implicit def f6: OFormat[Configuration.Network] = Json.format[Configuration.Network]

    implicit def f7: OFormat[Configuration.NetworkIOMutation] = Json.format[Configuration.NetworkIOMutation]

    implicit def f8: OFormat[Configuration.NetworkMutation] = Json.format[Configuration.NetworkMutation]

    implicit def f9: OFormat[Configuration.Adaptation] = Json.format[Configuration.Adaptation]

    implicit def f10: OFormat[Configuration.Simulation] = Json.format[Configuration.Simulation]

    implicit def f11: OFormat[Configuration] = Json.format[Configuration]
  }

}
