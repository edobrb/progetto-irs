import analysis.Functions._
import model.config.Config
import utils.Argos

object Main extends App {

  val WORKING_DIR = "/home/edo/Desktop/progetto-irs"
  val SIMULATION_FILE = "config_simulation.argos"

  /** Simulation configuration (will reflect on the .argos file and robots parameters) **/
  val simulation = Config.Simulation(
    ticks_per_seconds = 10,
    experiment_length = 720,
    network_test_steps = 400,
    print_analytics = true)
  val robot = Config.Robot(
    proximity_threshold = 0.1,
    max_wheel_speed = 1.5)
  val bnOptions = Config.BooleanNetwork.Options(
    node_count = 100,
    nodes_input_count = 3,
    bias = 0.79,
    network_inputs_count = 24,
    network_outputs_count = 2,
    self_loops = false,
    override_output_nodes_bias = true
  )
  val bn = Config.BooleanNetwork(
    max_input_rewires = 3,
    input_rewires_probability = 0.5,
    max_output_rewires = 1,
    output_rewires_probability = 0.1,
    use_dual_encoding = false,
    options = bnOptions)
  val config = Config(simulation, robot, bn)

  /** Simulation run **/
  val output = Argos.runConfiguredSimulation(WORKING_DIR, SIMULATION_FILE, config)

  /** Parsing each robot's output for each step (mapped in a sequence of StepInfo) **/
  val result = extractTests(output.map(toStepInfo).collect { case Some(info) => info })

  //TODO: save and analyze the results
  println(result.size)

  val max = result.maxBy(_._2.map(_.fitnessValues.last).max)
  val maxT = max._2.maxBy(_.fitnessValues.last)
  println(maxT)
}