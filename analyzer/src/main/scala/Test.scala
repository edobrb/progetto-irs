import model.config.Config
import utils.{Argos, Hash}


object Test extends App {

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
    bias = 0.1, //0.1, 0.5, 0.79
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
    options = bnOptions)
  val config = Config(simulation, robot, bn)

  val configs = Seq(
    config.copy(bn = bn.copy(options = bnOptions.copy(bias = 0.1))),
    config.copy(bn = bn.copy(options = bnOptions.copy(bias = 0.5))),
    config.copy(bn = bn.copy(options = bnOptions.copy(bias = 0.79))))

  /** Simulation standard output (by lines) **/
  def output(config: Config): LazyList[String] = Argos.runConfiguredSimulation(WORKING_DIR, SIMULATION_FILE, config)

  def runExperiment(config: Config, times: Int, offset: Int = 0): Unit = {
    val name = Hash.sha256(config.toJson)
    1 to times foreach (i => {
      val startTime = System.currentTimeMillis()
      val expectedLines = config.simulation.experiment_length * config.simulation.ticks_per_seconds * 10
      println(s"Running test $i with bias ${config.bn.options.bias}")
      val out = LazyList(config.toJson) ++ output(config).zipWithIndex.map {
        case (str, i) if i % 1000 == 0 =>
          println(s"Running test $i with bias ${config.bn.options.bias}: $i / $expectedLines => ${i * 100.0 / expectedLines}%")
          str
        case (str, _) => str
      }
      utils.File.writeGzippedLines("/home/edo/Desktop/progetto-irs/tmp/" + name + "-" + (i + offset), out)
      val dt = System.currentTimeMillis() - startTime
      println(s"Done test $i with bias ${config.bn.options.bias} (${dt / 1000} s)")
    })
  }

  import scala.collection.parallel.CollectionConverters._
  configs.par.foreach(c => {
    runExperiment(c, 1)
  })


  /*val asd = utils.File.readGzippedLines("/home/edo/Desktop/progetto-irs/tmp/61a833d4b3401c162acbe806ebe5008499f06a8cf60b524b9a33a9a31ec35e9d-4").map {
    case (value, source) =>
      val config = Config.fromJson(value.head)
      val results = extractTests(value.map(toStepInfo).collect { case Some(info) => info })
      source.close()
      (config, results)
  }
  println(asd)*/
}
