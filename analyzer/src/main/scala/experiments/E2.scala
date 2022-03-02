package experiments

import model.config.Configuration.{Adaptation, Forwarding, HalfRegionVariation, Network, NetworkIO, NetworkIOMutation, NetworkMutation, Objective, ObstacleAvoidance, Simulation}
import model.config.{Configuration, Variation}
import utils.ConfigLens._

/**
 * Investigate the effect of p, io rewires, network mutation and both
 * in full and half arena.
 */
object E2 extends ExperimentSettings {

  def defaultConfig: Configuration = Configuration(
    Simulation(
      argos = "experiments/parametrized.argos",
      ticks_per_seconds = 10,
      experiment_length = 7200 * 2,
      robot_count = 10,
      print_analytics = true),
    Adaptation(epoch_length = 80,
      NetworkMutation(
        max_connection_rewires = 0,
        connection_rewire_probability = 1,
        self_loops = false,
        only_distinct_connections = true,
        max_function_bit_flips = 0,
        function_bit_flips_probability = 1,
        keep_p_balance = false),
      NetworkIOMutation(
        max_input_rewires = 0,
        input_rewire_probability = 1,
        max_output_rewires = 0,
        output_rewire_probability = 1,
        allow_io_node_overlap = false)),
    Network(n = 100, k = 3, p = 0, self_loops = false, only_distinct_connections = true,
      io = NetworkIO(
        override_output_nodes = true,
        override_outputs_p = 0.5,
        allow_io_node_overlap = false),
      initial_schema = None,
      initial_state = None),
    Objective(
      Forwarding(max_wheel_speed = 25, wheels_nodes = 2),
      ObstacleAvoidance(proximity_threshold = 0.1, proximity_nodes = 8),
      None)
  )


  /** Configuration variations */
  def configVariation: Seq[Variation[Configuration, _]] = {
    val ioLens = lens(_.adaptation.network_io_mutation.max_input_rewires) and lens(_.adaptation.network_io_mutation.max_output_rewires)
    val netLens = lens(_.adaptation.network_mutation.max_connection_rewires) and lens(_.adaptation.network_mutation.max_function_bit_flips)
    Seq(
      /*Variation[Configuration, String](Seq("experiments/parametrized.argos"/*, "experiments/parametrized-random-cylinder.argos"*/), lens(_.simulation.argos), "Arena", {
        case "experiments/parametrized.argos" => "[Arena rectangle]"
        case "experiments/parametrized-random-cylinder.argos" => "[Arena cylinder]"
      }),*/
      Variation(Seq(0.1, 0.5, 0.79), lens(_.network.p), "p"),
      Variation[Configuration, ((Int, Int), (Int, Int))](Seq(((2, 1), (0, 0)), ((0, 0), (3, 8)), ((2, 1), (3, 8))), ioLens and netLens, "adaptation", {
        case ((2, 1), (0, 0)) => "rewire"
        case ((0, 0), (3, 8)) => "mutation"
        case ((2, 1), (3, 8)) => "rewire-and-mutation"
      }),
      Variation[Configuration, Option[HalfRegionVariation]](Seq(None,
        Some(HalfRegionVariation(region_nodes = 1, reset_region_every_epoch = false))),
        lens(_.objective.half_region_variation), "objective", {
          case None => "whole"
          case Some(HalfRegionVariation(1, _, _)) => "half"
        })
    )
  }
}
