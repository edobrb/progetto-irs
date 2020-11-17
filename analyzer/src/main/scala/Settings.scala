import model.config
import model.config.{Configuration, Variation}
import model.config.Configuration.{Adaptation, Forwarding, HalfRegionVariation, Network, NetworkIO, NetworkIOMutation, NetworkMutation, Objective, ObstacleAvoidance, Simulation}
import monocle.Lens
import monocle.macros.GenLens
import utils.ConfigLens._

import scala.util.Try
import monocle.macros.syntax.lens._

object Settings {

  /** Default simulation configuration (will reflect on the .argos file and robots parameters) */
  def DEFAULT_CONFIG: Configuration = Configuration(
    Simulation(
      argos = "experiments/parametrized.argos",
      ticks_per_seconds = 10,
      experiment_length = 7200,
      robot_count = 10,
      print_analytics = true),
    Adaptation(epoch_length = 400,
      NetworkMutation(
        max_connection_rewires = 0,
        connection_rewire_probability = 1,
        self_loops = false,
        max_function_bit_flips = 0,
        function_bit_flips_probability = 1,
        keep_p_balance = false),
      NetworkIOMutation(max_input_rewires = 2,
        input_rewire_probability = 1,
        max_output_rewires = 1,
        output_rewire_probability = 1,
        allow_io_node_overlap = false)),
    Network(n = 100, k = 3, p = 0.79, self_loops = true,
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
  def variations: Seq[Variation[Configuration, _]] = Seq(
    Variation(Seq(0.1, 0.5, 0.79), lens(_.network.p), "p"),
    Variation(Seq(0, 1), lens(_.adaptation.network_io_mutation.max_output_rewires), "or"),
    Variation(Seq(true, false), lens(_.network.self_loops), "sl", collapse = true),
    Variation(Seq(8, 24), lens(_.objective.obstacle_avoidance.proximity_nodes), "pn"),
    Variation(Seq(None,
      Some(HalfRegionVariation(region_nodes = 1, reset_region_every_epoch = false)),
      Some(HalfRegionVariation(region_nodes = 0, reset_region_every_epoch = false))),
      lens(_.objective.half_region_variation), "v", (v: Option[HalfRegionVariation]) => v match {
        case None => "whole arena"
        case Some(HalfRegionVariation(1, _, _)) => "half arena - feed"
        case Some(HalfRegionVariation(0, _, _)) => "half arena - no feed"
      })
  )

  /** All configuration combinations */
  def configurations: Seq[Configuration] =
    utils.Combiner(DEFAULT_CONFIG, variations.map(_.apply)).distinct

  /** Filenames of experiments and the relative config */
  def experiments(implicit args: Array[String]): Seq[(String, Configuration, Int)] = {
    /** Configuration repetitions for statistical accuracy. */
    configurations.flatMap {
      config =>
        def setSeed(i: Int): Configuration = {
          val name = config.filename + "-" + i
          config
            .setSimulationSeed(Some(Math.abs((name + "-simulation").hashCode)))
            .setControllersSeed(Some(Math.abs((name + "-controller").hashCode)))
        }

        Args.REPETITIONS.map(i => (config.filename + "-" + i, setSeed(i), i))
    }
  }
}
