import model.config.{Configuration, Variation}
import model.config.Configuration._
import monocle.Lens
import utils.ConfigLens._
import utils.Parallel.Parallel

object TestController extends App {

  implicit val arguments: Array[String] = args

  def DEFAULT_CONFIG: Configuration = Configuration(
    Simulation(
      argos = "experiments/parametrized.argos",
      ticks_per_seconds = 10,
      experiment_length = 1200,
      robot_count = 10,
      print_analytics = true),
    Adaptation(epoch_length = 10,
      NetworkMutation(
        max_connection_rewires = 0,
        connection_rewire_probability = 0,
        self_loops = false,
        only_distinct_connections = false,
        max_function_bit_flips = 0,
        function_bit_flips_probability = 0,
        keep_p_balance = false),
      NetworkIOMutation(max_input_rewires = 0,
        input_rewire_probability = 0,
        max_output_rewires = 0,
        output_rewire_probability = 0,
        allow_io_node_overlap = false)),
    Network(n = 100, k = 3, p = 0.79, self_loops = true, only_distinct_connections = false,
      io = NetworkIO(
        override_output_nodes = true,
        override_outputs_p = 0.5,
        allow_io_node_overlap = false),
      initial_schema = None,
      initial_state = None),
    Objective(
      Forwarding(max_wheel_speed = 50, wheels_nodes = 2),
      ObstacleAvoidance(proximity_threshold = 0.1, proximity_nodes = 8),
      Some(HalfRegionVariation(1, 0.1, reset_region_every_epoch = false)))
  )

  case class Test[T](result: String, lens: Lens[Configuration, T], value: T, simulationSeed: Int = 1, controllerSeed: Int = 2) {
    def run(): Unit = {
      val (_, time) = utils.Benchmark.time {
        val c = lens.set(value)(DEFAULT_CONFIG).setControllersSeed(Some(controllerSeed)).setSimulationSeed(Some(simulationSeed))
        val exp1 = Experiments.runSimulation(c, visualization = false).filter(_.headOption.contains('{')).drop(1)
        val out = utils.Hash.sha256(exp1)
        print(s"Test $result -> $out" + (if (result == out) " [SUCCESS]" else " [FAILURE]"))
      }
      println(s" ($time)")
    }
  }

  val irLens = lens(_.adaptation.network_io_mutation.input_rewire_probability) and lens(_.adaptation.network_io_mutation.max_input_rewires)
  val orLens = lens(_.adaptation.network_io_mutation.output_rewire_probability) and lens(_.adaptation.network_io_mutation.max_output_rewires)
  val crLens = lens(_.adaptation.network_mutation.connection_rewire_probability) and lens(_.adaptation.network_mutation.max_connection_rewires)
  val rOverlapLens = lens(_.adaptation.network_io_mutation.allow_io_node_overlap)
  val tests = Seq(
    Test("d77aa95002b5219852f913216bf705fd235ea557908c8bc7232c6ef9a4c08e72", crLens, (1.0, 20)),
    Test("5fd4dbf5e4d460a3850d43aca4ccf81ab5ea5b5d8da209f4559b00eb1ffbe6fa", lens(_.adaptation.network_mutation.connection_rewire_probability), 1.0),
    Test("5fd4dbf5e4d460a3850d43aca4ccf81ab5ea5b5d8da209f4559b00eb1ffbe6fa", lens(_.adaptation.network_mutation.function_bit_flips_probability), 1.0),
    Test("a90b8fd0fa2ac74edee2a0e158d0d817718c34f1db3eddaa842d6f191a752feb", lens(_.network.io.override_outputs_p), 0.25),
    Test("5fd4dbf5e4d460a3850d43aca4ccf81ab5ea5b5d8da209f4559b00eb1ffbe6fa", lens(_.network.io.override_outputs_p), 0.5),
    Test("5fd4dbf5e4d460a3850d43aca4ccf81ab5ea5b5d8da209f4559b00eb1ffbe6fa", lens(_.network.io.override_output_nodes), true),
    Test("8ed687852a7108b293ba135e4970ac7ed77bbba0acb0b96b35be9be2d1879895", lens(_.network.io.override_output_nodes), false),
    Test("8d3b3f30423a1b0158c0a9cc06cc6af14289ffd58ff38a12ce8421deb719ba30", lens(_.network.io.allow_io_node_overlap), true),
    Test("5fd4dbf5e4d460a3850d43aca4ccf81ab5ea5b5d8da209f4559b00eb1ffbe6fa", lens(_.network.io.allow_io_node_overlap), false),
    Test("5fd4dbf5e4d460a3850d43aca4ccf81ab5ea5b5d8da209f4559b00eb1ffbe6fa", lens(_.network.self_loops), true),
    Test("ffc9880ccd113b48d37d0ca9e98a3276cc8e3bc31cb83f043c08feabc97d3113", lens(_.network.self_loops), false),
    Test("118fcdd1ce57537b1ddaa46443f533e2db48f68d5d42ff86e2c7b6deef379a7f", lens(_.network.k), 2),
    Test("d2a80b52bc2ec76cecabc673f06b75ef2b54b75ee36f1926655a6cef0daafb79", lens(_.network.k), 4),
    Test("5fd4dbf5e4d460a3850d43aca4ccf81ab5ea5b5d8da209f4559b00eb1ffbe6fa", lens(_.network.k), 3),
    Test("e927c99a5a7c70f24ced01d42d09e7f37aafd38ece40e70781e7fac6a6c5ff92", lens(_.network.n), 50),
    Test("a607c4a9240e0991cd2906cd7314e6ced2775bd2b2b3f3eae88c542ea3b6f5cf", lens(_.network.n), 150),
    Test("5fd4dbf5e4d460a3850d43aca4ccf81ab5ea5b5d8da209f4559b00eb1ffbe6fa", lens(_.network.n), 100),
    Test("fcba94072ddff1d995a559ba6508261ec68bdd53a24c7778e44e18623e229ad1", lens(_.network.p), 0.1),
    Test("2dd75936a53d49773c7734e4a96db66cf8450d97c0f3a692a4e589abf5a48332", lens(_.network.p), 0.5),
    Test("5fd4dbf5e4d460a3850d43aca4ccf81ab5ea5b5d8da209f4559b00eb1ffbe6fa", lens(_.network.p), 0.79),
    Test("bfeb3c7f18478f0132ad1e0c21c4531448b74d0e09e7b2ca300b29962947ea93", orLens and rOverlapLens, ((0.5, 2), true)),
    Test("2cdaa052282ec749b700a6efde9c69c82f9ceaf598efcf8b6c59d4616680f771", irLens and rOverlapLens, ((0.5, 2), true)),
    Test("5fd4dbf5e4d460a3850d43aca4ccf81ab5ea5b5d8da209f4559b00eb1ffbe6fa", irLens, (1.0, 0)),
    Test("5fd4dbf5e4d460a3850d43aca4ccf81ab5ea5b5d8da209f4559b00eb1ffbe6fa", irLens, (0.0, 0)),
    Test("f2845602b80fe9a395431e69f9bd58218a2ef0e853ee26c8d1da18b5909dc8b3", irLens, (0.6, 2)),
    Test("beb59b96899b443487b06df49ee2d1ed9b9d52c538893d580e167d55b82f3f83", irLens, (0.5, 2)),
    Test("5fd4dbf5e4d460a3850d43aca4ccf81ab5ea5b5d8da209f4559b00eb1ffbe6fa", orLens, (1.0, 0)),
    Test("5fd4dbf5e4d460a3850d43aca4ccf81ab5ea5b5d8da209f4559b00eb1ffbe6fa", orLens, (0.0, 0)),
    Test("79a22be104eec9ab0a6fd2d025fdd83304322ef6648499274ae6f68952f323b5", orLens, (0.6, 2)),
    Test("649d3d9aa61af56e3139cae65a998b35d1f8f862633e50331179153fc45f0e1d", orLens, (0.5, 2)),
  )
  tests.parForeach(Args.PARALLELISM_DEGREE, _.run())
}
