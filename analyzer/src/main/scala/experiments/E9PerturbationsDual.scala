package experiments

import model.config.Configuration._
import model.config.{Configuration, Variation}
import utils.ConfigLens._

/**
 * Investigate the effect of p, io rewires, network mutation and both
 * in full, half arena and foraging arena.
 */
object E9PerturbationsDual extends ExperimentSettings {

  def defaultConfig: Configuration = Configuration(
    Simulation(
      argos = "",
      ticks_per_seconds = 10,
      experiment_length = 80 * 200, //200 edits
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

    val argosLens = lens(_.simulation.argos)
    val halfLens = lens(_.objective.half_region_variation)
    val foragingSettings = Map("variant" -> "foraging", "light_nodes" -> "8", "light_threshold" -> "0.1")

    Seq(
      Variation(Seq(0.9, 0.21), lens(_.network.p), "p"),

      Variation.apply2[Configuration, ((Int, Int), (Int, Int))](Seq(((2, 1), (0, 0)), ((2, 1), (3, 8))), ioLens and netLens, "adaptation", "", {
        case ((2, 1), (0, 0)) => "ripartizione"
        case ((2, 1), (3, 8)) => "ibrida"
      }),

      Variation.normal2[Configuration, String](Seq("whole", "half", "foraging", "foraging2"), {
        case ("whole", config) =>
          argosLens.set("experiments/parametrized.argos")(config)
        case ("half", config) =>
          val halfVariation = Some(HalfRegionVariation(region_nodes = 1, reset_region_every_epoch = true, penalty_factor = -1))
          halfLens.set(halfVariation)(argosLens.set("experiments/parametrized.argos")(config))
        case ("foraging", config) =>
          argosLens.set("experiments/parametrized-foraging.argos")(config).copy(other = foragingSettings ++ config.other)
        case ("foraging2", config) =>
          argosLens.set("experiments/parametrized-foraging2.argos")(config).copy(other = foragingSettings ++ config.other)
      }, {
        case config if config.simulation.argos == "experiments/parametrized.argos" && config.objective.half_region_variation.isEmpty => "I"
        case config if config.simulation.argos == "experiments/parametrized.argos" && config.objective.half_region_variation.isDefined => "II"
        case config if config.simulation.argos == "experiments/parametrized-foraging.argos" => "III"
        case config if config.simulation.argos == "experiments/parametrized-foraging2.argos" => "IV"
      }, "objective", "Arena", identity),

      Variation.normal2[Configuration, Option[String]](Seq(None, Some("100")), {
        case (v, config) => v match {
          case Some(value) => config.copy(other = config.other.updated("states_flip_f", value))
          case None => config
        }
      }, _.other.get("states_flip_f"), "flips", "\uD835\uDF08", {
        case Some(value) => value.toDouble.toInt.toString
        case None => "0"
      }),
    )
  }
}
