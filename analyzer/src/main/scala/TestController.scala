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

  case class Test[T](lens: Lens[Configuration, T], value: T, result: String, simulationSeed: Int = 1, controllerSeed: Int = 2) {
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
    Test(crLens, (1.0, 20), "10fcad5ffd374cbccf8bc54dcdc42c13e78953c5d03fff1a06eb5f174c6bdc93"),
    Test(lens(_.adaptation.network_mutation.connection_rewire_probability), 1.0, "4f84146b1fb051849a6c087e7a3f688d08b04728451b882ef89e1df5ba326703"),
    Test(lens(_.adaptation.network_mutation.function_bit_flips_probability), 1.0, "4f84146b1fb051849a6c087e7a3f688d08b04728451b882ef89e1df5ba326703"),
    Test(lens(_.network.io.override_outputs_p), 0.25, "639271eb9fca73f31d9dcfa175ff1e32053c5e6a494a080042e0ae4b6bb425c4"),
    Test(lens(_.network.io.override_outputs_p), 0.5, "4f84146b1fb051849a6c087e7a3f688d08b04728451b882ef89e1df5ba326703"),
    Test(lens(_.network.io.override_output_nodes), true, "4f84146b1fb051849a6c087e7a3f688d08b04728451b882ef89e1df5ba326703"),
    Test(lens(_.network.io.override_output_nodes), false, "4bcc93832943a10acf5cd4dcee5a8ec515ea3f576e2dd402486ba3cb35c62665"),
    Test(lens(_.network.io.allow_io_node_overlap), true, "253313d3467db5aca8338970c7229fffef6ef1958d7a7bf2678eb379d6732834"),
    Test(lens(_.network.io.allow_io_node_overlap), false, "4f84146b1fb051849a6c087e7a3f688d08b04728451b882ef89e1df5ba326703"),
    Test(lens(_.network.self_loops), true, "4f84146b1fb051849a6c087e7a3f688d08b04728451b882ef89e1df5ba326703"),
    Test(lens(_.network.self_loops), false, "7fbcc0d917199a3ba11bb6b2b2023b384c17b209762d8a4bb19cc0d0f7dfa753"),
    Test(lens(_.network.k), 2, "54b9e8a22e483d051b3ccd7b4b202f3086ff4689d5452b4f098754190854b897"),
    Test(lens(_.network.k), 4, "35813d172df51e2f5cb07ecdaa5f064eec6859f31fff73c5e67fa1ac5c0905ac"),
    Test(lens(_.network.k), 3, "4f84146b1fb051849a6c087e7a3f688d08b04728451b882ef89e1df5ba326703"),
    Test(lens(_.network.n), 50, "130a6e29ceaebe05ab83871cc34b1ac259907cbbf1c01e93fefd1fee61c3569d"),
    Test(lens(_.network.n), 150, "d928127807d2ed5e93ad96869fb35536df6ce3561bd75f16c87b1491d26345dd"),
    Test(lens(_.network.n), 100, "4f84146b1fb051849a6c087e7a3f688d08b04728451b882ef89e1df5ba326703"),
    Test(lens(_.network.p), 0.1, "6cce39befef6ed05fe662ba573d2fe06fb0a5d95283aa79cb9f8d156166c8b85"),
    Test(lens(_.network.p), 0.5, "d93be49a019026b93e8dc56f1d7082a9d332cc207fd7d66308aae681f87190f1"),
    Test(lens(_.network.p), 0.79, "4f84146b1fb051849a6c087e7a3f688d08b04728451b882ef89e1df5ba326703"),
    Test(orLens and rOverlapLens, ((0.5, 2), true), "8c98164f4f5087e8fa0a0ba57d35a261dd9bcc92cb0e69ffbb8d028e4a08fab1"),
    Test(irLens and rOverlapLens, ((0.5, 2), true), "4eaa339c27e3cd9a9d60db156a2dc884d0bd4d2bd7c019e13a81caefebc2098e"),
    Test(irLens, (1.0, 0), "4f84146b1fb051849a6c087e7a3f688d08b04728451b882ef89e1df5ba326703"),
    Test(irLens, (0.0, 0), "4f84146b1fb051849a6c087e7a3f688d08b04728451b882ef89e1df5ba326703"),
    Test(irLens, (0.6, 2), "83338984c7ad4d19f3679acdaa2b28075cecbaaf0872637f8d058c624bbd9786"),
    Test(irLens, (0.5, 2), "d1b95bb35e0b72fed45052c0c1dc84baec9710277810389342925c748cfbae26"),
    Test(orLens, (1.0, 0), "4f84146b1fb051849a6c087e7a3f688d08b04728451b882ef89e1df5ba326703"),
    Test(orLens, (0.0, 0), "4f84146b1fb051849a6c087e7a3f688d08b04728451b882ef89e1df5ba326703"),
    Test(orLens, (0.6, 2), "4533a71d9f07f17b0288df88f18e7c29ae7347d6ff2b0c6f7a7d394e74bc4cc0"),
    Test(orLens, (0.5, 2), "f842ef0ae50d5249082b4e05d8eacd596fe2d29a8cc77dc3d60dc3d77a63993d"),
  )
  tests.parForeach(Args.PARALLELISM_DEGREE, _.run())
}
