import model.config.Configuration
import model.config.Configuration.{Adaptation, Forwarding, HalfRegionVariation, Network, NetworkIO, NetworkIOMutation, NetworkMutation, Objective, ObstacleAvoidance, Simulation}

import scala.util.Try

object Settings {

  def argOrDefault[T](argName: String, f: String => Option[T], default: T)(args: Array[String]): T =
    argOrException(argName, f, Some(default))(args)

  def argOrException[T](argName: String, f: String => Option[T], default: Option[T] = None)(args: Array[String]): T =
    args.find(a => Try {
      a.split('=')(0) == argName
    }.toOption.contains(true)).flatMap(a => Try {
      a.split('=')(1)
    }.toOption).flatMap(f) match {
      case Some(value) => value
      case None => default match {
        case Some(value) => value
        case None => throw new Exception(s"Argument $argName not defined")
      }
    }

  def WORKING_DIR(implicit args: Array[String]): String = argOrException("working_dir", Some.apply)(args)

  def SIMULATION_FILE(implicit args: Array[String]): String = argOrException("argos", Some.apply)(args)

  def DATA_FOLDER(implicit args: Array[String]): String = argOrException("data", Some.apply)(args)

  def PARALLELISM_DEGREE(implicit args: Array[String]): Int = argOrDefault("threads", v => Try(v.toInt).toOption, 4)(args)

  def REPETITIONS(implicit args: Array[String]): Range =
    argOrDefault("from", v => Try(v.toInt).toOption, 1)(args) to argOrDefault("to", v => Try(v.toInt).toOption, 100)(args)

  /** Default simulation configuration (will reflect on the .argos file and robots parameters) */
  def DEFAULT_CONFIG: Configuration = Configuration(
    Simulation(
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

  def configurations: Seq[Configuration] = {
    /** Configuration variations */
    def biasVariation: Seq[Configuration => Configuration] = Seq(
      c => c.copy(network = c.network.copy(p = 0.1)),
      c => c.copy(network = c.network.copy(p = 0.5)),
      c => c.copy(network = c.network.copy(p = 0.79)),
    )

    def outputRewiresVariation: Seq[Configuration => Configuration] = Seq(
      c => c.copy(adaptation = c.adaptation.copy(network_io_mutation = c.adaptation.network_io_mutation.copy(max_output_rewires = 1))),
      c => c.copy(adaptation = c.adaptation.copy(network_io_mutation = c.adaptation.network_io_mutation.copy(max_output_rewires = 0))),
    )

    def selfLoopVariation: Seq[Configuration => Configuration] = Seq(
      c => c.copy(network = c.network.copy(self_loops = true)),
      c => c.copy(network = c.network.copy(self_loops = false)),
    )

    def stayOnHalfVariation: Seq[Configuration => Configuration] = Seq(
      c => c.copy(objective = c.objective.copy(half_region_variation = None)),
      c => c.copy(objective = c.objective.copy(half_region_variation = Some(HalfRegionVariation(region_nodes = 1, reset_region_every_epoch = false)))),
      c => c.copy(objective = c.objective.copy(half_region_variation = Some(HalfRegionVariation(region_nodes = 0, reset_region_every_epoch = false)))),
    )

    def networkInputCountVariation: Seq[Configuration => Configuration] = Seq(
      c => c.copy(objective = c.objective.copy(obstacle_avoidance = c.objective.obstacle_avoidance.copy(proximity_nodes = 8))),
      c => c.copy(objective = c.objective.copy(obstacle_avoidance = c.objective.obstacle_avoidance.copy(proximity_nodes = 24))),
    )

    val variations = Seq(biasVariation, outputRewiresVariation, selfLoopVariation, stayOnHalfVariation, networkInputCountVariation)
    utils.Combiner(DEFAULT_CONFIG, variations)
  }

  /** Filenames of experiments and the relative config */
  def experiments(implicit args: Array[String]): Seq[(String, Configuration, Int)] = {
    /** Configuration repetitions for statistical accuracy. * */
    configurations.zipWithIndex.flatMap({
      case (config, index) =>
        def setSeed(i: Int): Configuration = {
          val name = config.filename + "-" + i
          config
            .setSimulationSeed(Some(Math.abs((name + "-simulation").hashCode)))
            .setControllersSeed(Some(Math.abs((name + "-controller").hashCode)))
        }

        REPETITIONS.map(i => (config.filename + "-" + i, setSeed(i), i))
    })
  }
}
