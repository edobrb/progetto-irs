import java.io.File

import analysis.Functions._
import model.config.Config

import scala.sys.process.Process

object Main extends App {

  val simulation = Config.Simulation( //seed, robot number
    ticks_per_seconds = 10,
    experiment_length = 720,
    network_test_steps = 400,
    print_analytics = true)
  val robot = Config.Robot(
    proximity_threshold = 0.1,
    max_wheel_speed = 1.5)
  val bnOptions =  Config.BooleanNetwork.Options(
    node_count = 100,
    nodes_input_count = 3,
    bias = 0.79,
    network_inputs_count = 24,
    network_outputs_count = 2,
    self_loops = false,
    override_output_nodes_bias = true
  )
  val bn = Config.BooleanNetwork(options = bnOptions)
  val config = Config(simulation, robot, bn)


  def runSimulation(workingDir: String, simulationFile: String): LazyList[String] =
    Process(s"argos3 -c $simulationFile", new File(workingDir)).lazyLines

  def runSimulation2(workingDir: String, simulationFile: String, config: Config): LazyList[String] = {

    Process(s"./pargos $simulationFile --CONFIG=${\"}\\'${config.toJson.replaceAll("\"","\\\"")}\\'\" --TICKS=${config.simulation.ticks_per_seconds} --LENGTH=${config.simulation.experiment_length}",
      new File(workingDir)).lazyLines
  }

  val lines = runSimulation2(workingDir = "/home/edo/Desktop/progetto-irs", simulationFile = "config_simulation.argos", config)
  val result = extractTests(lines.map(toStepInfo).collect { case Some(info) => info })

  println(config.toJson)
  println(result.size)
}