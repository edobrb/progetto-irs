import model.config
import model.config.Configuration
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
  def variations: Seq[Seq[Configuration => Configuration]] = Seq(
    Seq(0.1, 0.5, 0.79).lensMap(lens(_.network.p)),
    Seq(0, 1).lensMap(lens(_.adaptation.network_io_mutation.max_output_rewires)),
    Seq(true, false).lensMap(lens(_.network.self_loops)),
    Seq(8, 24).lensMap(lens(_.objective.obstacle_avoidance.proximity_nodes)),
    Seq(None,
      Some(HalfRegionVariation(region_nodes = 1, reset_region_every_epoch = false)),
      Some(HalfRegionVariation(region_nodes = 0, reset_region_every_epoch = false)))
      .lensMap(lens(_.objective.half_region_variation)))

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

  /** All configuration combinations */
  def configurations: Seq[Configuration] =
    utils.Combiner(DEFAULT_CONFIG, variations)

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

        REPETITIONS.map(i => (config.filename + "-" + i, setSeed(i), i))
    }
  }
}
