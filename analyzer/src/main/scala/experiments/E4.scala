package experiments

import model.config.Configuration._
import model.config.{Configuration, Variation}
import utils.ConfigLens._

/**
 * Investigate the effect of penalty_factor and reset_region_every_epoch
 * in half arena with no mutation.
 */
object E4 extends ExperimentSettings {

  def defaultConfig: Configuration = Configuration(
    Simulation(
      argos = "experiments/parametrized.argos",
      ticks_per_seconds = 10,
      experiment_length = 7200 * 2 * 10,
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
        max_input_rewires = 2,
        input_rewire_probability = 1,
        max_output_rewires = 1,
        output_rewire_probability = 1,
        allow_io_node_overlap = false)),
    Network(n = 100, k = 3, p = 0.79, self_loops = false, only_distinct_connections = true,
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
    lens(_.objective.half_region_variation)
    Seq(
      Variation[Configuration, Option[HalfRegionVariation]](Seq(
        Some(HalfRegionVariation(region_nodes = 1, reset_region_every_epoch = true, penalty_factor = 0)),
        Some(HalfRegionVariation(region_nodes = 1, reset_region_every_epoch = true, penalty_factor = -1)),
        Some(HalfRegionVariation(region_nodes = 1, reset_region_every_epoch = false, penalty_factor = 0)),
        Some(HalfRegionVariation(region_nodes = 1, reset_region_every_epoch = false, penalty_factor = -1)),
      ), lens(_.objective.half_region_variation), "", {
        case Some(HalfRegionVariation(_, f, reset)) => s"penalty=$f,reset=$reset"
      }),
    )
  }
}