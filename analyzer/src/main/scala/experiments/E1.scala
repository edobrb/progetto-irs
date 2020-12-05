package experiments

import model.config.Configuration._
import model.config.{Configuration, Variation}
import utils.ConfigLens._

/**
 * Investigate the effect of p, max_output_rewires, self_loops, proximity_nodes
 * in both full arena and half arena. No network mutation.
 */
object E1 extends ExperimentSettings {

  def defaultConfig: Configuration = Configuration(
    Simulation(
      argos = "experiments/parametrized.argos",
      ticks_per_seconds = 10,
      experiment_length = 7200,
      robot_count = 10,
      print_analytics = true),
    Adaptation(epoch_length = 40,
      NetworkMutation(
        max_connection_rewires = 0,
        connection_rewire_probability = 1,
        self_loops = false,
        only_distinct_connections = true,
        max_function_bit_flips = 0,
        function_bit_flips_probability = 1,
        keep_p_balance = false),
      NetworkIOMutation(
        max_input_rewires = 2,
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
      Forwarding(max_wheel_speed = 50, wheels_nodes = 2),
      ObstacleAvoidance(proximity_threshold = 0.1, proximity_nodes = 8),
      None)
  )


  /** Configuration variations */
  def configVariation: Seq[Variation[Configuration, _]] = Seq(
    Variation(Seq(0.1, 0.5, 0.79), lens(_.network.p), "p"),
    Variation(Seq(0, 1), lens(_.adaptation.network_io_mutation.max_output_rewires), "or"),
    Variation(Seq(true, false), lens(_.network.self_loops), "sl", collapse = true),
    Variation(Seq(8, 24), lens(_.objective.obstacle_avoidance.proximity_nodes), "pn"),
    Variation(Seq(None,
      Some(HalfRegionVariation(region_nodes = 1, reset_region_every_epoch = false)),
      Some(HalfRegionVariation(region_nodes = 0, reset_region_every_epoch = false))),
      lens(_.objective.half_region_variation), "objective", (v: Option[HalfRegionVariation]) => v match {
        case None => "whole"
        case Some(HalfRegionVariation(1, _, _)) => "half - feed"
        case Some(HalfRegionVariation(0, _, _)) => "half - no feed"
      })
  )
}