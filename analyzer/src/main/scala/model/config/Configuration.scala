package model.config

import model.BooleanNetwork
import model.config.Configuration._
import play.api.libs.json.{Json, OFormat}
import utils.ConfigLens.lens

case class Configuration(simulation: Simulation,
                         adaptation: Adaptation,
                         network: Network,
                         objective: Objective,
                         other: Map[String, String] = Map()) {
  def toJson: String = {
    import JsonFormats._
    Json.toJson(this).toString()
  }

  def expectedLines: Int = {
    val argosInfoPrints = 0
    val initialConfigPrints = 1
    val stepPrints = simulation.experiment_length * simulation.ticks_per_seconds * simulation.robot_count
    val initialBnConfigPrints = 2 * stepPrints / (adaptation.epoch_length * simulation.ticks_per_seconds)
    stepPrints + argosInfoPrints + initialConfigPrints + initialBnConfigPrints
  }

  def filename: String =
    utils.Hash.sha256(setSimulationSeed(None).setControllersSeed(None).toString)

  def setSeed(i: Int): Configuration = {
    val name = this.filename + "-" + i
    this
      .setSimulationSeed(Some(Math.abs((name + "-simulation").hashCode)))
      .setControllersSeed(Some(Math.abs((name + "-controller").hashCode)))
  }

  def setSimulationSeed(seed: Option[Int]): Configuration =
    lens(_.simulation.simulation_random_seed).set(seed)(this)

  def setControllersSeed(seed: Option[Int]): Configuration =
    lens(_.simulation.controllers_random_seed).set(seed)(this)

  def setInitialSchema(schema: Option[BooleanNetwork]): Configuration =
    lens(_.network.initial_schema).set(schema.map(_.copy(states = IndexedSeq.empty)))(this)
}

object Configuration {

  case class Simulation(argos: String,
                        ticks_per_seconds: Int,
                        experiment_length: Int,
                        robot_count: Int,
                        print_analytics: Boolean,
                        controllers_random_seed: Option[Int] = None,
                        simulation_random_seed: Option[Int] = None)

  case class Adaptation(epoch_length: Int,
                        network_mutation: NetworkMutation, //selection, crossover?
                        network_io_mutation: NetworkIOMutation)

  case class NetworkMutation(max_connection_rewires: Int,
                             connection_rewire_probability: Double,
                             self_loops: Boolean,
                             only_distinct_connections: Boolean,
                             max_function_bit_flips: Int,
                             function_bit_flips_probability: Double,
                             keep_p_balance: Boolean)

  case class NetworkIOMutation(max_input_rewires: Int,
                               input_rewire_probability: Double,
                               max_output_rewires: Int,
                               output_rewire_probability: Double,
                               allow_io_node_overlap: Boolean)

  case class Network(n: Int,
                     k: Int,
                     p: Double,
                     self_loops: Boolean,
                     only_distinct_connections: Boolean,
                     io: NetworkIO,
                     initial_schema: Option[BooleanNetwork] = None,
                     initial_state: Option[Seq[Boolean]] = None)

  case class NetworkIO(override_output_nodes: Boolean,
                       override_outputs_p: Double,
                       allow_io_node_overlap: Boolean)

  case class Objective(forwarding: Forwarding,
                       obstacle_avoidance: ObstacleAvoidance,
                       half_region_variation: Option[HalfRegionVariation])

  case class Forwarding(max_wheel_speed: Double,
                        wheels_nodes: Int)

  case class ObstacleAvoidance(proximity_threshold: Double,
                               proximity_nodes: Int)

  case class HalfRegionVariation(region_nodes: Int,
                                 penalty_factor: Double = 0,
                                 reset_region_every_epoch: Boolean)


  def fromJson(json: String): Configuration = {
    import JsonFormats._
    Json.fromJson[Configuration](Json.parse(json)).get
  }

  object JsonFormats {
    implicit def f0: OFormat[BooleanNetwork] = Json.format[BooleanNetwork]

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
