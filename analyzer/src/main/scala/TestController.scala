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
    Adaptation(epoch_length = 100,
      NetworkMutation(
        max_connection_rewires = 0,
        connection_rewire_probability = 0,
        self_loops = false,
        max_function_bit_flips = 0,
        function_bit_flips_probability = 0,
        keep_p_balance = false),
      NetworkIOMutation(max_input_rewires = 0,
        input_rewire_probability = 0,
        max_output_rewires = 0,
        output_rewire_probability = 0,
        allow_io_node_overlap = false)),
    Network(n = 100, k = 3, p = 0.79, self_loops = true,
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
  val rOverlapLens = lens(_.adaptation.network_io_mutation.allow_io_node_overlap)
  val tests = Seq(
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
  tests.parForeach(1, _.run())
  /*val configurations: Seq[(Configuration, String)] = Seq(
    (Configuration(
      Simulation(argos = "experiments/parametrized.argos", ticks_per_seconds = 10, experiment_length = 1200, robot_count = 10, print_analytics = true, Some(1), Some(2)),
      Adaptation(epoch_length = 400, NetworkMutation(0, 1, self_loops = false, 0, 1, keep_p_balance = false), NetworkIOMutation(2, 1, 2, 1, allow_io_node_overlap = false)),
      Network(n = 100, k = 3, p = 0.79, self_loops = true, NetworkIO(override_output_nodes = true, override_outputs_p = 0.5, allow_io_node_overlap = false)),
      Objective(Forwarding(50, 2), ObstacleAvoidance(0.1, 8), Some(HalfRegionVariation(1, 0.1, reset_region_every_epoch = false)))
    ), "4a4d355b1c2f8eddaf1e9fce04dfcb1fc294d6c02fe7c5ac011f1e5f7111e0fe"),
    (Configuration(
      Simulation(argos = "experiments/parametrized.argos", ticks_per_seconds = 10, experiment_length = 1200, robot_count = 10, print_analytics = true, Some(1), Some(2)),
      Adaptation(epoch_length = 400, NetworkMutation(0, 1, self_loops = false, 0, 1, keep_p_balance = false), NetworkIOMutation(2, 0.5, 2, 0.5, allow_io_node_overlap = false)),
      Network(n = 100, k = 3, p = 0.79, self_loops = true, NetworkIO(override_output_nodes = true, override_outputs_p = 0.5, allow_io_node_overlap = false)),
      Objective(Forwarding(5, 4), ObstacleAvoidance(0.1, 6), Some(HalfRegionVariation(1, 0.1, reset_region_every_epoch = false)))
    ), "5577153557ac03f9201c8f4def31cc1c7fcbf57a56433b0adbdffcbf84624147"),
    (Configuration(
      Simulation(argos = "experiments/parametrized.argos", ticks_per_seconds = 10, experiment_length = 1200, robot_count = 10, print_analytics = true, Some(15), Some(16)),
      Adaptation(epoch_length = 400, NetworkMutation(0, 1, self_loops = false, 0, 1, keep_p_balance = false), NetworkIOMutation(2, 0.5, 2, 0.5, allow_io_node_overlap = false)),
      Network(n = 100, k = 3, p = 0.79, self_loops = false, NetworkIO(override_output_nodes = true, override_outputs_p = 0.5, allow_io_node_overlap = false)),
      Objective(Forwarding(5, 4), ObstacleAvoidance(0.1, 6), Some(HalfRegionVariation(1, 0.1, reset_region_every_epoch = false)))
    ), "d6bd6df16426448b4adfe4454a624d1d8ef9ff1970520b37802b74d8c2d29812"),
    (Configuration(
      Simulation(argos = "experiments/parametrized.argos", ticks_per_seconds = 10, experiment_length = 1200, robot_count = 10, print_analytics = true, Some(3), Some(4)),
      Adaptation(epoch_length = 400, NetworkMutation(0, 1, self_loops = false, 0, 1, keep_p_balance = false), NetworkIOMutation(2, 0.5, 2, 0.5, allow_io_node_overlap = true)),
      Network(n = 100, k = 3, p = 0.79, self_loops = true, NetworkIO(override_output_nodes = true, override_outputs_p = 0.5, allow_io_node_overlap = false)),
      Objective(Forwarding(5, 4), ObstacleAvoidance(0.1, 6), Some(HalfRegionVariation(1, 0.1, reset_region_every_epoch = false)))
    ), "7f794451892a766f76bb10b0b0a87e8b5eda2ec8dbfe0231a2af726ad7fdab91"),
    (Configuration(
      Simulation(argos = "experiments/parametrized.argos", ticks_per_seconds = 10, experiment_length = 1200, robot_count = 10, print_analytics = true, Some(5), Some(6)),
      Adaptation(epoch_length = 400, NetworkMutation(0, 1, self_loops = false, 0, 1, keep_p_balance = false), NetworkIOMutation(2, 0.5, 2, 0.5, allow_io_node_overlap = false)),
      Network(n = 100, k = 3, p = 0.79, self_loops = true, NetworkIO(override_output_nodes = true, override_outputs_p = 0.5, allow_io_node_overlap = true)),
      Objective(Forwarding(5, 4), ObstacleAvoidance(0.1, 6), Some(HalfRegionVariation(1, 0.1, reset_region_every_epoch = false)))
    ), "a926ecd7273090d729ea3f59557c62c9da4ddaff6568dc0f397f4e4083125d41"),
    (Configuration(
      Simulation(argos = "experiments/parametrized.argos", ticks_per_seconds = 10, experiment_length = 1200, robot_count = 10, print_analytics = true, Some(7), Some(8)),
      Adaptation(epoch_length = 400, NetworkMutation(0, 1, self_loops = false, 0, 1, keep_p_balance = false), NetworkIOMutation(2, 0.5, 2, 0.5, allow_io_node_overlap = false)),
      Network(n = 100, k = 3, p = 0.79, self_loops = true, NetworkIO(override_output_nodes = true, override_outputs_p = 0.5, allow_io_node_overlap = false)),
      Objective(Forwarding(5, 4), ObstacleAvoidance(0.1, 6), Some(HalfRegionVariation(1, 0.1, reset_region_every_epoch = true)))
    ), "18cc55d7bc2fb2496345b097cea2ac4b4170951024c9a8a65213c1c3d9981ebd"),
    (Configuration(
      Simulation(argos = "experiments/parametrized.argos", ticks_per_seconds = 10, experiment_length = 1200, robot_count = 12, print_analytics = true, Some(9), Some(10)),
      Adaptation(epoch_length = 400, NetworkMutation(10, 0.5, self_loops = false, 10, 0.5, keep_p_balance = false), NetworkIOMutation(0, 0, 0, 0, allow_io_node_overlap = false)),
      Network(n = 100, k = 3, p = 0.79, self_loops = true, NetworkIO(override_output_nodes = true, override_outputs_p = 0.5, allow_io_node_overlap = false)),
      Objective(Forwarding(5, 4), ObstacleAvoidance(0.1, 6), Some(HalfRegionVariation(1, 0.1, reset_region_every_epoch = false)))
    ), "10825855de61b2cce932676c70fd7c048e94fbb27688011b8498a39ce6818c25"),
    (Configuration(
      Simulation(argos = "experiments/parametrized.argos", ticks_per_seconds = 10, experiment_length = 1200, robot_count = 12, print_analytics = true, Some(11), Some(12)),
      Adaptation(epoch_length = 400, NetworkMutation(10, 0.5, self_loops = false, 10, 0.5, keep_p_balance = true), NetworkIOMutation(0, 0, 0, 0, allow_io_node_overlap = false)),
      Network(n = 100, k = 3, p = 0.79, self_loops = true, NetworkIO(override_output_nodes = true, override_outputs_p = 0.5, allow_io_node_overlap = false)),
      Objective(Forwarding(5, 4), ObstacleAvoidance(0.1, 6), Some(HalfRegionVariation(1, 0.1, reset_region_every_epoch = false)))
    ), "41aef79ad7cb58a7292a1eb967d9f2288029d478cac2dd3e64b038daa115a312"),
    (Configuration(
      Simulation(argos = "experiments/parametrized.argos", ticks_per_seconds = 10, experiment_length = 1200, robot_count = 12, print_analytics = true, Some(13), Some(14)),
      Adaptation(epoch_length = 400, NetworkMutation(10, 0.5, self_loops = false, 10, 0.5, keep_p_balance = true), NetworkIOMutation(2, 1, 2, 1, allow_io_node_overlap = false)),
      Network(n = 100, k = 3, p = 0.79, self_loops = true, NetworkIO(override_output_nodes = true, override_outputs_p = 0.25, allow_io_node_overlap = false)),
      Objective(Forwarding(5, 4), ObstacleAvoidance(0.1, 6), Some(HalfRegionVariation(1, 0.1, reset_region_every_epoch = false)))
    ), "3e81146b7cea4325c4d26e2562d90b025c16e979f30fbe3ed75d8eab0b11a569"),
    (Configuration(
      Simulation(argos = "experiments/parametrized.argos", ticks_per_seconds = 10, experiment_length = 1200, robot_count = 12, print_analytics = true, Some(17), Some(18)),
      Adaptation(epoch_length = 400, NetworkMutation(10, 0.5, self_loops = false, 10, 0.5, keep_p_balance = true), NetworkIOMutation(2, 1, 2, 1, allow_io_node_overlap = false)),
      Network(n = 100, k = 3, p = 0.79, self_loops = true, NetworkIO(override_output_nodes = false, override_outputs_p = 0.25, allow_io_node_overlap = false)),
      Objective(Forwarding(5, 4), ObstacleAvoidance(0.1, 6), Some(HalfRegionVariation(1, 0.1, reset_region_every_epoch = false)))
    ), "764729550527694ac47579ac576065f0f9710e0d8d52c756271b9e4cf620d44e"),
    (Configuration(
      Simulation(argos = "experiments/parametrized.argos", ticks_per_seconds = 10, experiment_length = 1200, robot_count = 12, print_analytics = true, Some(19), Some(20)),
      Adaptation(epoch_length = 400, NetworkMutation(10, 0.5, self_loops = false, 10, 0.5, keep_p_balance = true), NetworkIOMutation(2, 1, 2, 1, allow_io_node_overlap = false)),
      Network(n = 100, k = 3, p = 0.79, self_loops = true, NetworkIO(override_output_nodes = true, override_outputs_p = 0.5, allow_io_node_overlap = false)),
      Objective(Forwarding(5, 2), ObstacleAvoidance(0.1, 6), Some(HalfRegionVariation(1, 0.1, reset_region_every_epoch = false)))
    ), "bb70dd09169e696c5dfd5b3995117be75727473520a17c9a28995d5727e48f42"),
    (Configuration(
      Simulation(argos = "experiments/parametrized.argos", ticks_per_seconds = 10, experiment_length = 1200, robot_count = 12, print_analytics = true, Some(21), Some(22)),
      Adaptation(epoch_length = 400, NetworkMutation(10, 0.5, self_loops = false, 10, 0.5, keep_p_balance = true), NetworkIOMutation(2, 1, 2, 1, allow_io_node_overlap = false)),
      Network(n = 100, k = 3, p = 0.79, self_loops = true, NetworkIO(override_output_nodes = true, override_outputs_p = 0.5, allow_io_node_overlap = false)),
      Objective(Forwarding(5, 4), ObstacleAvoidance(0.1, 6), Some(HalfRegionVariation(2, 0, reset_region_every_epoch = false)))
    ), "a75d052ab9577d13f02379afeb97afbc44bedc8274293d26e703d808297c24f8"),
    (Configuration(
      Simulation(argos = "experiments/parametrized.argos", ticks_per_seconds = 10, experiment_length = 1200, robot_count = 12, print_analytics = true, Some(21), Some(22)),
      Adaptation(epoch_length = 400, NetworkMutation(10, 0.5, self_loops = false, 10, 0.5, keep_p_balance = true), NetworkIOMutation(2, 0.9, 2, 1, allow_io_node_overlap = false)),
      Network(n = 100, k = 3, p = 0.79, self_loops = true, NetworkIO(override_output_nodes = true, override_outputs_p = 0.5, allow_io_node_overlap = false)),
      Objective(Forwarding(5, 4), ObstacleAvoidance(0.1, 6), Some(HalfRegionVariation(2, 0, reset_region_every_epoch = false)))
    ), "2446594f972be9d5c7d80c7f1b6397f18448fe7a3b5a427589804c6dcac2c86c"),
    (Configuration(
      Simulation(argos = "experiments/parametrized.argos", ticks_per_seconds = 10, experiment_length = 1200, robot_count = 12, print_analytics = true, Some(21), Some(22)),
      Adaptation(epoch_length = 400, NetworkMutation(10, 0.5, self_loops = false, 10, 0.5, keep_p_balance = true), NetworkIOMutation(2, 1, 2, 0.9, allow_io_node_overlap = false)),
      Network(n = 100, k = 3, p = 0.79, self_loops = true, NetworkIO(override_output_nodes = true, override_outputs_p = 0.5, allow_io_node_overlap = false)),
      Objective(Forwarding(5, 4), ObstacleAvoidance(0.1, 6), Some(HalfRegionVariation(2, 0, reset_region_every_epoch = false)))
    ), "6d7af56dc9849acd02dbdf5434e013faade75deb313b597fcb7520d56211ff90"),
    (Configuration(
      Simulation(argos = "experiments/parametrized.argos", ticks_per_seconds = 10, experiment_length = 1200, robot_count = 12, print_analytics = true, Some(21), Some(22)),
      Adaptation(epoch_length = 400, NetworkMutation(10, 0.4, self_loops = false, 10, 0.5, keep_p_balance = true), NetworkIOMutation(2, 1, 2, 1, allow_io_node_overlap = false)),
      Network(n = 100, k = 3, p = 0.79, self_loops = true, NetworkIO(override_output_nodes = true, override_outputs_p = 0.5, allow_io_node_overlap = false)),
      Objective(Forwarding(5, 4), ObstacleAvoidance(0.1, 6), Some(HalfRegionVariation(2, 0, reset_region_every_epoch = false)))
    ), "5400c2f87afe0602f9d4ac03e6752c60d1b0088ca3ca4b6d7dc183b5b3f67e39"),
    (Configuration(
      Simulation(argos = "experiments/parametrized.argos", ticks_per_seconds = 10, experiment_length = 1200, robot_count = 12, print_analytics = true, Some(21), Some(22)),
      Adaptation(epoch_length = 400, NetworkMutation(10, 0.5, self_loops = false, 10, 0.4, keep_p_balance = true), NetworkIOMutation(2, 1, 2, 1, allow_io_node_overlap = false)),
      Network(n = 100, k = 3, p = 0.79, self_loops = true, NetworkIO(override_output_nodes = true, override_outputs_p = 0.5, allow_io_node_overlap = false)),
      Objective(Forwarding(5, 4), ObstacleAvoidance(0.1, 6), Some(HalfRegionVariation(2, 0, reset_region_every_epoch = false)))
    ), "2ba441d3a2ec4553a160d99f8257f92324f1c5662fd3bea46afa7524bb0e5a74"),
    (Configuration(
      Simulation(argos = "experiments/parametrized.argos", ticks_per_seconds = 10, experiment_length = 1200, robot_count = 12, print_analytics = true, Some(21), Some(22)),
      Adaptation(epoch_length = 400, NetworkMutation(10, 0.5, self_loops = true, 10, 0.4, keep_p_balance = true), NetworkIOMutation(2, 1, 2, 1, allow_io_node_overlap = false)),
      Network(n = 100, k = 3, p = 0.79, self_loops = true, NetworkIO(override_output_nodes = true, override_outputs_p = 0.5, allow_io_node_overlap = false)),
      Objective(Forwarding(5, 4), ObstacleAvoidance(0.1, 6), Some(HalfRegionVariation(2, 0, reset_region_every_epoch = false)))
    ), "3f4a176f535352424b0f1a997f0d502b91aac77348127b73b754fa77bf0cb903")
  )

  configurations.drop(0).zipWithIndex.foreach {
    case ((config, hash), i) =>
      val (_, time) = utils.Benchmark.time {
        print(s"Executing test $i... ")
        val exp1 = Experiments.runSimulation(config, visualization = false).filter(_.headOption.contains('{')).drop(1)
        val result = utils.Hash.sha256(exp1)
        print(result + " == " + hash + (if (result == hash) " [SUCCESS]" else " [FAILURE]"))
      }
      println(s" ($time)")
  }
*/
}
