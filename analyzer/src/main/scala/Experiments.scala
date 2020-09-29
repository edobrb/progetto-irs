import model.config.Config
import model.config.Config.JsonFormats._
import utils.Parallel._
import utils.RichIterator._
import utils.{Argos, Benchmark}

import scala.util.Random

object Experiments extends App {

  def WORKING_DIR = "/home/edo/Desktop/progetto-irs"

  def SIMULATION_FILE = "config_simulation.argos"

  def DATA_FOLDER = "/mnt/hgfs/data"

  /** Default simulation configuration (will reflect on the .argos file and robots parameters) **/
  def DEFAULT_CONFIG: Config = {
    def simulation = Config.Simulation(
      ticks_per_seconds = 10,
      experiment_length = 7200,
      network_test_steps = 400,
      override_robot_count = None,
      print_analytics = true)

    def robot = Config.Robot(
      proximity_threshold = 0.1,
      max_wheel_speed = 5,
      stay_on_half = false,
      feed_position = false)

    def bnOptions = Config.BooleanNetwork.Options(
      node_count = 100,
      nodes_input_count = 3,
      bias = 0.79,
      network_inputs_count = 24,
      network_outputs_count = 2,
      self_loops = false,
      override_output_nodes_bias = true)

    def bn = Config.BooleanNetwork(
      max_input_rewires = 2,
      input_rewires_probability = 1,
      max_output_rewires = 0,
      output_rewires_probability = 1,
      use_dual_encoding = false,
      options = bnOptions,
      initial = None)

    Config(simulation, robot, bn)
  }

  /** Function that map configuration to name **/
  def configName(config: Config): (String, Config) = {
    object Params {
      def unapply(arg: Config): Option[(Int, Int, Double, Int, Int, Boolean, Int, Boolean, Boolean)] =
        Some((arg.simulation.experiment_length,
          arg.simulation.robot_count,
          arg.bn.options.bias,
          arg.bn.max_input_rewires,
          arg.bn.max_output_rewires,
          arg.bn.options.self_loops,
          arg.bn.options.network_inputs_count,
          arg.robot.stay_on_half,
          arg.robot.stay_on_half && arg.robot.feed_position))
    }
    (config match {
      case Params(7200, 10, 0.1, 2, 0, false, 24, false, false) => "default-b0.1-or0-ir2"
      case Params(7200, 10, 0.5, 2, 0, false, 24, false, false) => "default-b0.5-or0-ir2"
      case Params(7200, 10, 0.79, 2, 0, false, 24, false, false) => "default-b0.79-or0-ir2"
      case Params(7200, 10, 0.1, 2, 1, false, 24, false, false) => "default-b0.1-or1-ir2"
      case Params(7200, 10, 0.5, 2, 1, false, 24, false, false) => "default-b0.5-or1-ir2"
      case Params(7200, 10, 0.79, 2, 1, false, 24, false, false) => "default-b0.79-or1-ir2"
      case Params(7200, 10, 0.1, 2, 0, true, 24, false, false) => "default-b0.1-or0-ir2-sl"
      case Params(7200, 10, 0.5, 2, 0, true, 24, false, false) => "default-b0.5-or0-ir2-sl"
      case Params(7200, 10, 0.79, 2, 0, true, 24, false, false) => "default-b0.79-or0-ir2-sl"
      case Params(7200, 10, 0.1, 2, 1, true, 24, false, false) => "default-b0.1-or1-ir2-sl"
      case Params(7200, 10, 0.5, 2, 1, true, 24, false, false) => "default-b0.5-or1-ir2-sl"
      case Params(7200, 10, 0.79, 2, 1, true, 24, false, false) => "default-b0.79-or1-ir2-sl"

      case Params(7200, 10, 0.1, 2, 0, false, 24, true, false) => "half-b0.1-or0-ir2"
      case Params(7200, 10, 0.5, 2, 0, false, 24, true, false) => "half-b0.5-or0-ir2"
      case Params(7200, 10, 0.79, 2, 0, false, 24, true, false) => "half-b0.79-or0-ir2"
      case Params(7200, 10, 0.1, 2, 1, false, 24, true, false) => "half-b0.1-or1-ir2"
      case Params(7200, 10, 0.5, 2, 1, false, 24, true, false) => "half-b0.5-or1-ir2"
      case Params(7200, 10, 0.79, 2, 1, false, 24, true, false) => "half-b0.79-or1-ir2"
      case Params(7200, 10, 0.1, 2, 0, true, 24, true, false) => "half-b0.1-or0-ir2-sl"
      case Params(7200, 10, 0.5, 2, 0, true, 24, true, false) => "half-b0.5-or0-ir2-sl"
      case Params(7200, 10, 0.79, 2, 0, true, 24, true, false) => "half-b0.79-or0-ir2-sl"
      case Params(7200, 10, 0.1, 2, 1, true, 24, true, false) => "half-b0.1-or1-ir2-sl"
      case Params(7200, 10, 0.5, 2, 1, true, 24, true, false) => "half-b0.5-or1-ir2-sl"
      case Params(7200, 10, 0.79, 2, 1, true, 24, true, false) => "half-b0.79-or1-ir2-sl"

      case Params(7200, 10, 0.1, 2, 0, false, 8, false, false) => "default-b0.1-or0-ir2-nic8"
      case Params(7200, 10, 0.5, 2, 0, false, 8, false, false) => "default-b0.5-or0-ir2-nic8"
      case Params(7200, 10, 0.79, 2, 0, false, 8, false, false) => "default-b0.79-or0-ir2-nic8"
      case Params(7200, 10, 0.1, 2, 1, false, 8, false, false) => "default-b0.1-or1-ir2-nic8"
      case Params(7200, 10, 0.5, 2, 1, false, 8, false, false) => "default-b0.5-or1-ir2-nic8"
      case Params(7200, 10, 0.79, 2, 1, false, 8, false, false) => "default-b0.79-or1-ir2-nic8"
      case Params(7200, 10, 0.1, 2, 0, true, 8, false, false) => "default-b0.1-or0-ir2-sl-nic8"
      case Params(7200, 10, 0.5, 2, 0, true, 8, false, false) => "default-b0.5-or0-ir2-sl-nic8"
      case Params(7200, 10, 0.79, 2, 0, true, 8, false, false) => "default-b0.79-or0-ir2-sl-nic8"
      case Params(7200, 10, 0.1, 2, 1, true, 8, false, false) => "default-b0.1-or1-ir2-sl-nic8"
      case Params(7200, 10, 0.5, 2, 1, true, 8, false, false) => "default-b0.5-or1-ir2-sl-nic8"
      case Params(7200, 10, 0.79, 2, 1, true, 8, false, false) => "default-b0.79-or1-ir2-sl-nic8"

      case Params(7200, 10, 0.1, 2, 0, false, 8, true, false) => "half-b0.1-or0-ir2-nic8"
      case Params(7200, 10, 0.5, 2, 0, false, 8, true, false) => "half-b0.5-or0-ir2-nic8"
      case Params(7200, 10, 0.79, 2, 0, false, 8, true, false) => "half-b0.79-or0-ir2-nic8"
      case Params(7200, 10, 0.1, 2, 1, false, 8, true, false) => "half-b0.1-or1-ir2-nic8"
      case Params(7200, 10, 0.5, 2, 1, false, 8, true, false) => "half-b0.5-or1-ir2-nic8"
      case Params(7200, 10, 0.79, 2, 1, false, 8, true, false) => "half-b0.79-or1-ir2-nic8"
      case Params(7200, 10, 0.1, 2, 0, true, 8, true, false) => "half-b0.1-or0-ir2-sl-nic8"
      case Params(7200, 10, 0.5, 2, 0, true, 8, true, false) => "half-b0.5-or0-ir2-sl-nic8"
      case Params(7200, 10, 0.79, 2, 0, true, 8, true, false) => "half-b0.79-or0-ir2-sl-nic8"
      case Params(7200, 10, 0.1, 2, 1, true, 8, true, false) => "half-b0.1-or1-ir2-sl-nic8"
      case Params(7200, 10, 0.5, 2, 1, true, 8, true, false) => "half-b0.5-or1-ir2-sl-nic8"
      case Params(7200, 10, 0.79, 2, 1, true, 8, true, false) => "half-b0.79-or1-ir2-sl-nic8"


      case Params(7200, 10, 0.1, 2, 0, false, 24, true, true) => "half-b0.1-or0-ir2-feed"
      case Params(7200, 10, 0.5, 2, 0, false, 24, true, true) => "half-b0.5-or0-ir2-feed"
      case Params(7200, 10, 0.79, 2, 0, false, 24, true, true) => "half-b0.79-or0-ir2-feed"
      case Params(7200, 10, 0.1, 2, 1, false, 24, true, true) => "half-b0.1-or1-ir2-feed"
      case Params(7200, 10, 0.5, 2, 1, false, 24, true, true) => "half-b0.5-or1-ir2-feed"
      case Params(7200, 10, 0.79, 2, 1, false, 24, true, true) => "half-b0.79-or1-ir2-feed"
      case Params(7200, 10, 0.1, 2, 0, true, 24, true, true) => "half-b0.1-or0-ir2-sl-feed"
      case Params(7200, 10, 0.5, 2, 0, true, 24, true, true) => "half-b0.5-or0-ir2-sl-feed"
      case Params(7200, 10, 0.79, 2, 0, true, 24, true, true) => "half-b0.79-or0-ir2-sl-feed"
      case Params(7200, 10, 0.1, 2, 1, true, 24, true, true) => "half-b0.1-or1-ir2-sl-feed"
      case Params(7200, 10, 0.5, 2, 1, true, 24, true, true) => "half-b0.5-or1-ir2-sl-feed"
      case Params(7200, 10, 0.79, 2, 1, true, 24, true, true) => "half-b0.79-or1-ir2-sl-feed"

      case Params(7200, 10, 0.1, 2, 0, false, 8, true, true) => "half-b0.1-or0-ir2-nic8-feed"
      case Params(7200, 10, 0.5, 2, 0, false, 8, true, true) => "half-b0.5-or0-ir2-nic8-feed"
      case Params(7200, 10, 0.79, 2, 0, false, 8, true, true) => "half-b0.79-or0-ir2-nic8-feed"
      case Params(7200, 10, 0.1, 2, 1, false, 8, true, true) => "half-b0.1-or1-ir2-nic8-feed"
      case Params(7200, 10, 0.5, 2, 1, false, 8, true, true) => "half-b0.5-or1-ir2-nic8-feed"
      case Params(7200, 10, 0.79, 2, 1, false, 8, true, true) => "half-b0.79-or1-ir2-nic8-feed"
      case Params(7200, 10, 0.1, 2, 0, true, 8, true, true) => "half-b0.1-or0-ir2-sl-nic8-feed"
      case Params(7200, 10, 0.5, 2, 0, true, 8, true, true) => "half-b0.5-or0-ir2-sl-nic8-feed"
      case Params(7200, 10, 0.79, 2, 0, true, 8, true, true) => "half-b0.79-or0-ir2-sl-nic8-feed"
      case Params(7200, 10, 0.1, 2, 1, true, 8, true, true) => "half-b0.1-or1-ir2-sl-nic8-feed"
      case Params(7200, 10, 0.5, 2, 1, true, 8, true, true) => "half-b0.5-or1-ir2-sl-nic8-feed"
      case Params(7200, 10, 0.79, 2, 1, true, 8, true, true) => "half-b0.79-or1-ir2-sl-nic8-feed"


      case Params(7200, 10, 0.79, 2, 1, false, 8, false, false) => "default5-b0.79-or1-ir2"
      case Params(7200, 10, 0.79, 1, 1, false, 4, false, false) => "default6-b0.79-or1-ir1"
    }) -> config
  }


  /** Filenames of experiments and the relative config */
  def experiments: Map[String, Config] = {

    def configs: Map[String, Config] = {
      /** Configuration variations */
      def biasVariation: Seq[Config => Config] = Seq(
        c => c.copy(bn = c.bn.copy(options = c.bn.options.copy(bias = 0.1))),
        c => c.copy(bn = c.bn.copy(options = c.bn.options.copy(bias = 0.5))),
        c => c.copy(bn = c.bn.copy(options = c.bn.options.copy(bias = 0.79)))
      )

      def outputRewiresVariation: Seq[Config => Config] = Seq(
        c => c.copy(bn = c.bn.copy(max_output_rewires = 1)),
        c => c.copy(bn = c.bn.copy(max_output_rewires = 0))
      )

      def selfLoopVariation: Seq[Config => Config] = Seq(
        c => c.copy(bn = c.bn.copy(options = c.bn.options.copy(self_loops = true))),
        c => c.copy(bn = c.bn.copy(options = c.bn.options.copy(self_loops = false)))
      )

      def stayOnHalfVariation: Seq[Config => Config] = Seq(
        c => c.copy(robot = c.robot.copy(stay_on_half = false, feed_position = false)),
        c => c.copy(robot = c.robot.copy(stay_on_half = true, feed_position = false)),
        c => c.copy(robot = c.robot.copy(stay_on_half = true, feed_position = true))
      )

      def networkInputCountVariation: Seq[Config => Config] = Seq(
        c => c.copy(bn = c.bn.copy(options = c.bn.options.copy(network_inputs_count = 8))),
        c => c.copy(bn = c.bn.copy(options = c.bn.options.copy(network_inputs_count = 24)))
      )

      val variations = Seq(biasVariation, outputRewiresVariation, selfLoopVariation, stayOnHalfVariation, networkInputCountVariation)
      DEFAULT_CONFIG.combine(variations).map(configName).toMap
    }

    /** Configuration repetitions for statistical accuracy. **/
    configs.flatMap { case (experimentName, config) => (1 to 100
      ).map(i => (experimentName + "-" + i, config))
    }
  }

  /** Simulation standard output (by lines) **/
  def runSimulation(config: Config, visualization: Boolean = false): Iterator[String] =
    Argos.runConfiguredSimulation(WORKING_DIR, SIMULATION_FILE, config, visualization)

  /** Running experiments **/
  println(s"Running ${experiments.size} experiments...")
  experiments.toList.sortBy(_._1).parForeach(threads = 7, {
    case (experimentName, config) =>
      val filename = DATA_FOLDER + "/" + experimentName
      if (!utils.File.exists(filename)) {
        Thread.sleep(Random.nextInt(100))
        val expectedLines = config.simulation.experiment_length * config.simulation.ticks_per_seconds * config.simulation.robot_count
        Benchmark.time {
          println(s"Started experiment $experimentName ...")
          val out = config.toJson +: runSimulation(config)
          utils.File.writeGzippedLines(filename, out)
        } match {
          case (lines, time) => println(s"Done experiment $experimentName (${time.toSeconds} s, $lines/$expectedLines lines)")
        }
      } else {
        println("Skipping " + experimentName)
      }
  })
}
