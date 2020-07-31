import model.config.Config
import model.config.Config.JsonFormats._
import utils.{Argos, Benchmark}

import scala.collection.parallel.CollectionConverters._

object Experiments extends App {

  def WORKING_DIR = "/home/edo/Desktop/progetto-irs"

  def SIMULATION_FILE = "config_simulation.argos"

  def DATA_FOLDER = "/home/edo/Desktop/progetto-irs/tmp"

  def EXPERIMENT_REPETITION = 30

  /** Simulation configuration (will reflect on the .argos file and robots parameters) **/
  def simulation = Config.Simulation(
    ticks_per_seconds = 10,
    experiment_length = 7200,
    network_test_steps = 400,
    print_analytics = true)

  def robot = Config.Robot(
    proximity_threshold = 0.1,
    max_wheel_speed = 5)

  def bnOptions = Config.BooleanNetwork.Options(
    node_count = 100,
    nodes_input_count = 3,
    bias = 0.79, //0.1, 0.5, 0.79
    network_inputs_count = 24,
    network_outputs_count = 2,
    self_loops = false,
    override_output_nodes_bias = true)

  def bn = Config.BooleanNetwork(
    max_input_rewires = 2,
    input_rewires_probability = 1,
    max_output_rewires = 0,
    output_rewires_probability = 0,
    use_dual_encoding = false,
    options = bnOptions,
    initial = None)

  def defaultConfig: Config = Config(simulation, robot, bn)

  /** Configuration variations **/
  def biasVariation: Seq[Config => (String, Config)] = Seq(
    c => ("b0.1", c.changeBias(0.1)),
    c => ("b0.5", c.changeBias(0.5)),
    c => ("b0.79", c.changeBias(0.79)),
  )

  def outputRewiresVariation: Seq[Config => (String, Config)] = Seq(
    c => ("or1", c.copy(bn = c.bn.copy(max_output_rewires = 1))),
    //c => ("or0", c.copy(bn = c.bn.copy(max_output_rewires = 0))),
  )
  def inputRewiresVariation: Seq[Config => (String, Config)] = Seq(
    c => ("ir2", c.copy(bn = c.bn.copy(max_input_rewires = 2))),
    //c => ("ir1", c.copy(bn = c.bn.copy(max_input_rewires = 1))),
  )

  @scala.annotation.tailrec
  def combineConfigVariations(configs: Map[String, Config], variations: Seq[Seq[Config => (String, Config)]]): Map[String, Config] = {
    variations match {
      case Nil => configs
      case variation :: tail =>
        val newConfigs = configs.flatMap {
          case (name, config) => variation.map(_.apply(config)).map {
            case (str, config) => (name + "-" + str, config)
          }
        }
        combineConfigVariations(newConfigs, tail)
    }
  }

  /** All configurations based on defaultConfig and configurations variations **/
  def configs: Map[String, Config] = combineConfigVariations(Map("default" -> defaultConfig),
    Seq(biasVariation, outputRewiresVariation, inputRewiresVariation))

  /** Simulation standard output (by lines) **/
  def runSimulation(config: Config, visualization: Boolean = false): LazyList[String] =
    Argos.runConfiguredSimulation(WORKING_DIR, SIMULATION_FILE, config, visualization)

  def runExperiments(name: String, config: Config, offset: Int = 0): Unit = {
    1 to EXPERIMENT_REPETITION foreach (t => {
      val expectedLines = config.simulation.experiment_length * config.simulation.ticks_per_seconds * 10
      Benchmark.time {
        println(s"Running test $t with bias ${config.bn.options.bias}")
        val out = LazyList(config.toJson) ++ runSimulation(config).zipWithIndex.map {
          case (str, i) if i % 1000 == 0 =>
            println(s"Running test $t with bias ${config.bn.options.bias}: $i / $expectedLines => ${i * 100.0 / expectedLines}%")
            str
          case (str, _) => str
        }
        utils.File.writeGzippedLines(DATA_FOLDER + "/" + name + "-" + (t + offset), out)
      } match {
        case (_, time) => println(s"Done test $t with bias ${config.bn.options.bias} (${time.toSeconds} s)")
      }
    })
  }


  configs.par.foreach {
    case (name, config) => runExperiments(name, config, 0)
  }
}
