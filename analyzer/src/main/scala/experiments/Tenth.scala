package experiments

import model.config.Configuration._
import model.config.{Configuration, Variation}
import utils.ConfigLens._

/**
 * Investigate the effect of p, io rewires, network mutation and both
 * in full, half arena and foraging arena. (Rewire for half experiment, mutation for last half).
 */
object Tenth extends ExperimentSettings {

  def defaultConfig: Configuration = Configuration(
    Simulation(
      argos = "",
      ticks_per_seconds = 10,
      experiment_length = 80 * 200, //200 edits
      robot_count = 10,
      print_analytics = true),
    Adaptation(epoch_length = 80,
      NetworkMutation(
        max_connection_rewires = 3,
        connection_rewire_probability = 1,
        self_loops = false,
        only_distinct_connections = true,
        max_function_bit_flips = 8,
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
      None),
    other = Map("half_rewire_mutation" -> "")
  )

  //override def filter: Configuration => Boolean = c => c.network.p != 0.1 || c.network.io.override_output_nodes

  /** Configuration variations */
  def configVariation: Seq[Variation[Configuration, _]] = {
    val ioLens = lens(_.adaptation.network_io_mutation.max_input_rewires) and lens(_.adaptation.network_io_mutation.max_output_rewires)
    val netLens = lens(_.adaptation.network_mutation.max_connection_rewires) and lens(_.adaptation.network_mutation.max_function_bit_flips)

    val arenaLens = (lens(_.simulation.argos) and lens(_.other)) and lens(_.objective.half_region_variation)
    val wholeArena = (("experiments/parametrized.argos", Map[String, String]()), None)
    val halfArena = (("experiments/parametrized.argos", Map[String, String]()),
      Some(HalfRegionVariation(region_nodes = 1, reset_region_every_epoch = true, penalty_factor = -1)))
    val foragingArena = (("experiments/parametrized-foraging.argos", Map("half_rewire_mutation" -> "", "variant" -> "foraging", "light_nodes" -> "8", "light_threshold" -> "0.1")), None)
    Seq(
      Variation(Seq(0.1, 0.5, 0.79), lens(_.network.p), "p"),
      Variation(Seq(wholeArena, halfArena, foragingArena), arenaLens, "objective", (v: ((String, Map[String, String]), Option[HalfRegionVariation])) => v match {
        case (("experiments/parametrized.argos", _), None) => "whole"
        case (("experiments/parametrized.argos", _), Some(HalfRegionVariation(_, _, _))) => "half"
        case (("experiments/parametrized-foraging.argos", _), None) => "foraging"
      }),
    )
  }
}
