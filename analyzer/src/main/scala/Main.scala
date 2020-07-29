import analysis.Functions._
import model.{StepInfo, TestRun}
import model.config.Config
import utils.Argos

object Main extends App {

  val WORKING_DIR = "/home/edo/Desktop/progetto-irs"
  val SIMULATION_FILE = "config_simulation.argos"

  /** Simulation configuration (will reflect on the .argos file and robots parameters) **/
  val simulation = Config.Simulation(
    ticks_per_seconds = 10,
    experiment_length = 7200,
    network_test_steps = 400,
    print_analytics = true)
  val robot = Config.Robot(
    proximity_threshold = 0.1,
    max_wheel_speed = 5)
  val bnOptions = Config.BooleanNetwork.Options(
    node_count = 100,
    nodes_input_count = 3,
    bias = 0.79, //0.1, 0.5, 0.79
    network_inputs_count = 24,
    network_outputs_count = 2,
    self_loops = false,
    override_output_nodes_bias = true)
  val bn = Config.BooleanNetwork(
    max_input_rewires = 2,
    input_rewires_probability = 1,
    max_output_rewires = 0,
    output_rewires_probability = 1,
    use_dual_encoding = false,
    options = bnOptions,
    initial = None)
  val config = Config(simulation, robot, bn)

  /** Simulation standard output (by lines) **/
  def output: Iterable[String] = Argos.runConfiguredSimulation(WORKING_DIR, SIMULATION_FILE, config, visualization = false)

  /** Parse each robot's output for each step **/
  def data: Iterable[StepInfo] = output.map(toStepInfo).collect { case Some(info) => info }

  /** Collecting the results. For each robots will be generated a sequence of TestRun **/
  val result: Map[String, Seq[TestRun]] = extractTests(data)



  //TODO: save and analyze the results
  println(result.size)

  val (robotId, tests) = result.maxBy {
    case (robotId, tests) => tests.map(_.fitnessValues.last).max
  }
  val bestTest = tests.maxBy(_.fitnessValues.last)
  println(bestTest)
}