package experiments

import model.config.Configuration._
import model.config.{Configuration, Variation}
import utils.ConfigLens._

/**
 * Investigate the effect of k in critical and ordered network
 * with io rewires and with/without mutations.
 */
object E3 extends ExperimentSettings {

  def defaultConfig: Configuration = Configuration(
    Simulation(
      argos = "experiments/parametrized.argos",
      ticks_per_seconds = 10,
      experiment_length = 7200 * 2 * 10,
      robot_count = 10,
      print_analytics = true),
    Adaptation(epoch_length = 80,
      NetworkMutation(
        max_connection_rewires = 6,
        connection_rewire_probability = 1,
        self_loops = false,
        only_distinct_connections = true,
        max_function_bit_flips = 16,
        function_bit_flips_probability = 1,
        keep_p_balance = false),
      NetworkIOMutation(
        max_input_rewires = 2,
        input_rewire_probability = 1,
        max_output_rewires = 1,
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
    val netLens = lens(_.network.p) and lens(_.network.k)
    val netMutationLens = lens(_.adaptation.network_mutation.max_connection_rewires) and lens(_.adaptation.network_mutation.max_function_bit_flips)
    Seq(
      Variation[Configuration, (Int, Int)](Seq((6, 16), (0, 0)), netMutationLens, "adaptation", {
        case (0, 0) => "rewire"
        case (6, 16) => "rewire-and-mutation"
      }),
      Variation(Seq((0.1, 3), (0.79, 3), (0.1, 4), (0.852, 4)), netLens, "pk"),
      Variation[Configuration, Option[HalfRegionVariation]](Seq(None,
        Some(HalfRegionVariation(region_nodes = 1, reset_region_every_epoch = false))),
        lens(_.objective.half_region_variation), "objective", {
          case None => "whole"
          case Some(HalfRegionVariation(1, _, _)) => "half"
        })
    )
  }
}
