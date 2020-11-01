import model.config.Config

object Settings {

  def argOrException(index: Int, argName: String)(args: Array[String]): String =
    if (args.length > index) args(index) else throw new Exception(s"Argument $index should be $argName")

  def WORKING_DIR(implicit args: Array[String]): String = argOrException(0, "WORKING_DIR (lua)")(args)

  def SIMULATION_FILE(implicit args: Array[String]): String = argOrException(1, "SIMULATION_FILE (.argos)")(args)

  def DATA_FOLDER(implicit args: Array[String]): String = argOrException(2, "DATA_FOLDER")(args)

  def PARALLELISM_DEGREE(implicit args: Array[String]): Int = args.applyOrElse[Int, String](3, _ => "4").toInt

  /** Default simulation configuration (will reflect on the .argos file and robots parameters) */
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

  def configurations: Seq[Config] = {
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
    DEFAULT_CONFIG.combine(variations)
  }

  /** Filenames of experiments and the relative config */
  def experiments: Map[String, Config] = {
    /** Configuration repetitions for statistical accuracy. * */
    configurations.flatMap(config => (1 to 1).map(i => (config.filename + "-" + i, config))).toMap
  }
}
