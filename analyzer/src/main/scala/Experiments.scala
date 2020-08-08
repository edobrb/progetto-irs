import java.util.concurrent.{Executors, Future}

import model.config.Config
import model.config.Config.JsonFormats._
import utils.RichIterator._
import utils.{Argos, Benchmark}

import scala.util.Random

object Experiments extends App {

  def WORKING_DIR = "/home/edo/Desktop/progetto-irs"

  def SIMULATION_FILE = "config_simulation.argos"

  def DATA_FOLDER = "/mnt/hgfs/data"

  def EXPERIMENT_REPETITION = 100

  def EXPERIMENT_REPETITION_OFFSET = 0

  /** Simulation configuration (will reflect on the .argos file and robots parameters) **/
  def simulation = Config.Simulation(
    ticks_per_seconds = 10,
    experiment_length = 7200,
    network_test_steps = 400,
    override_robot_count = None,
    print_analytics = true)

  def robot = Config.Robot(
    proximity_threshold = 0.1,
    max_wheel_speed = 5)

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

  def defaultConfig: Config = Config(simulation, robot, bn)

  /** Configuration variations **/
  def biasVariation: Seq[Config => Config] = Seq(
    c => c.copy(bn = c.bn.copy(options = c.bn.options.copy(bias = 0.1))),
    c => c.copy(bn = c.bn.copy(options = c.bn.options.copy(bias = 0.5))),
    c => c.copy(bn = c.bn.copy(options = c.bn.options.copy(bias = 0.79))),
  )

  def outputRewiresVariation: Seq[Config => Config] = Seq(
    c => c.copy(bn = c.bn.copy(max_output_rewires = 1)),
    c => c.copy(bn = c.bn.copy(max_output_rewires = 0)),
  )

  def selfLoopVariation: Seq[Config => Config] = Seq(
    c => c.copy(bn = c.bn.copy(options = c.bn.options.copy(self_loops = true))),
    c => c.copy(bn = c.bn.copy(options = c.bn.options.copy(self_loops = false))),
  )

  @scala.annotation.tailrec
  def combineConfigVariations(configs: Seq[Config], variations: Seq[Seq[Config => Config]]): Seq[Config] = {
    variations match {
      case Nil => configs
      case variation :: tail =>
        val newConfigs = configs.flatMap(config => variation.map(_.apply(config)))
        combineConfigVariations(newConfigs, tail)
    }
  }

  /** Function that assign names to configs **/
  def configName(c: Config): (String, Config) = {
    object Params {
      def unapply(arg: Config): Option[(Int, Int, Double, Int, Int, Boolean)] =
        Some((arg.simulation.experiment_length, arg.simulation.robot_count, arg.bn.options.bias, arg.bn.max_input_rewires, arg.bn.max_output_rewires, arg.bn.options.self_loops))
    }
    (c match {
      case Params(7200, 10, 0.1, 2, 0, false) => "default-b0.1-or0-ir2"
      case Params(7200, 10, 0.5, 2, 0, false) => "default-b0.5-or0-ir2"
      case Params(7200, 10, 0.79, 2, 0, false) => "default-b0.79-or0-ir2"

      case Params(7200, 10, 0.1, 2, 1, false) => "default-b0.1-or1-ir2"
      case Params(7200, 10, 0.5, 2, 1, false) => "default-b0.5-or1-ir2"
      case Params(7200, 10, 0.79, 2, 1, false) => "default-b0.79-or1-ir2"

      case Params(7200, 10, 0.1, 2, 0, true) => "default-b0.1-or0-ir2-sl"
      case Params(7200, 10, 0.5, 2, 0, true) => "default-b0.5-or0-ir2-sl"
      case Params(7200, 10, 0.79, 2, 0, true) => "default-b0.79-or0-ir2-sl"

      case Params(7200, 10, 0.1, 2, 1, true) => "default-b0.1-or1-ir2-sl"
      case Params(7200, 10, 0.5, 2, 1, true) => "default-b0.5-or1-ir2-sl"
      case Params(7200, 10, 0.79, 2, 1, true) => "default-b0.79-or1-ir2-sl"

      case Params(14400, 20, 0.79, 2, 1, false) => "default2-b0.79-or1-ir2"

      case Params(10000, 10, 0.79, 2, 1, false) => "default3-b0.79-or1-ir2"

      case Params(40000, 10, 0.79, 2, 1, false) => "default4-b0.79-or1-ir2"
    }) -> c
  }

  /** All configurations based on defaultConfig and configurations variations **/
  def configs: Map[String, Config] = combineConfigVariations(Seq(defaultConfig),
    Seq(biasVariation, outputRewiresVariation, selfLoopVariation)).map(configName).toMap

  /** Filenames of experiments and the relative config **/
  def experiments: Map[String, Config] = configs.flatMap {
    case (experimentName, config) =>
      ((1 + EXPERIMENT_REPETITION_OFFSET) to EXPERIMENT_REPETITION).map(i => (experimentName + "-" + i, config))
  }

  /*def experiments: Map[String, Config] = (1 to 7).map(i => {
    val (name, config) = configName(defaultConfig.copy(simulation = defaultConfig.simulation.copy(experiment_length = 40000, override_robot_count = None),
      bn = defaultConfig.bn.copy(max_output_rewires = 1, options = defaultConfig.bn.options.copy(bias = 0.79, self_loops = false, node_count = 256, nodes_input_count = 5))))
    (name + "-" + i, config)
  }).toMap*/

  /** Simulation standard output (by lines) **/
  def runSimulation(config: Config, visualization: Boolean = false): Iterator[String] =
    Argos.runConfiguredSimulation(WORKING_DIR, SIMULATION_FILE, config, visualization)

  def runExperiments(): Unit = {
    val executor = Executors.newFixedThreadPool(7)

    val futures: Seq[Future[_]] = experiments.toList.sortBy(_._1).map {
      case (experimentName, config) =>
        executor.submit(new Runnable {
          override def run(): Unit = {
            val filename = DATA_FOLDER + "/" + experimentName
            if (!utils.File.exists(filename)) {
              Thread.sleep(Random.nextInt(100))
              val expectedLines = config.simulation.experiment_length * config.simulation.ticks_per_seconds * config.simulation.robot_count
              Benchmark.time {
                println(s"Started experiment $experimentName ...")
                val out = (config.toJson +: runSimulation(config)) /*.zipWithIndex.map {
              case (str, i) if i % 100000 == 0 =>
                println(s"Running experiment $experimentName: $i / $expectedLines => ${i * 100.0 / expectedLines}%")
                str
              case (str, _) => str
            }*/
                utils.File.writeGzippedLines(filename, out)
              } match {
                case (lines, time) => println(s"Done experiment $experimentName (${time.toSeconds} s, $lines/$expectedLines lines)")
              }
            } else {
              println("Skipping " + experimentName)
            }
          }
        })
    }
    futures.foreach(_.get())
    executor.shutdown()
  }

  println(s"Running ${experiments.size} experiments...")
  runExperiments()
}
