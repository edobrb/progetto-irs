import model.config.Configuration
import model.config.Configuration._

object TestController extends App {

  implicit val arguments: Array[String] = args

  val configurations: Seq[(Configuration,String)] = Seq(
    (Configuration(
      Simulation(ticks_per_seconds = 10, experiment_length = 1200, robot_count = 10, print_analytics = true, Some(1), Some(2)),
      Adaptation(epoch_length = 400, NetworkMutation(0, 1, self_loops = false, 0, 1, keep_p_balance = false), NetworkIOMutation(2, 1, 2, 1, allow_io_node_overlap = false)),
      Network(n = 100, k = 3, p = 0.79, self_loops = true, NetworkIO(override_output_nodes = true, override_outputs_p = 0.5, allow_io_node_overlap = false)),
      Objective(Forwarding(50, 2), ObstacleAvoidance(0.1, 8), Some(HalfRegionVariation(1, 0.1, reset_region_every_epoch = false)))
    ), "4a4d355b1c2f8eddaf1e9fce04dfcb1fc294d6c02fe7c5ac011f1e5f7111e0fe"),
      (Configuration(
      Simulation(ticks_per_seconds = 10, experiment_length = 1200, robot_count = 10, print_analytics = true, Some(1), Some(2)),
      Adaptation(epoch_length = 400, NetworkMutation(0, 1, self_loops = false, 0, 1, keep_p_balance = false), NetworkIOMutation(2, 0.5, 2, 0.5, allow_io_node_overlap = false)),
      Network(n = 100, k = 3, p = 0.79, self_loops = true, NetworkIO(override_output_nodes = true, override_outputs_p = 0.5, allow_io_node_overlap = false)),
      Objective(Forwarding(5, 4), ObstacleAvoidance(0.1, 6), Some(HalfRegionVariation(1, 0.1, reset_region_every_epoch = false)))
    ), "5577153557ac03f9201c8f4def31cc1c7fcbf57a56433b0adbdffcbf84624147"),
    (Configuration(
      Simulation(ticks_per_seconds = 10, experiment_length = 1200, robot_count = 10, print_analytics = true, Some(15), Some(16)),
      Adaptation(epoch_length = 400, NetworkMutation(0, 1, self_loops = false, 0, 1, keep_p_balance = false), NetworkIOMutation(2, 0.5, 2, 0.5, allow_io_node_overlap = false)),
      Network(n = 100, k = 3, p = 0.79, self_loops = false, NetworkIO(override_output_nodes = true, override_outputs_p = 0.5, allow_io_node_overlap = false)),
      Objective(Forwarding(5, 4), ObstacleAvoidance(0.1, 6), Some(HalfRegionVariation(1, 0.1, reset_region_every_epoch = false)))
    ), "d6bd6df16426448b4adfe4454a624d1d8ef9ff1970520b37802b74d8c2d29812"),
    (Configuration(
      Simulation(ticks_per_seconds = 10, experiment_length = 1200, robot_count = 10, print_analytics = true, Some(3), Some(4)),
      Adaptation(epoch_length = 400, NetworkMutation(0, 1, self_loops = false, 0, 1, keep_p_balance = false), NetworkIOMutation(2, 0.5, 2, 0.5, allow_io_node_overlap = true)),
      Network(n = 100, k = 3, p = 0.79, self_loops = true, NetworkIO(override_output_nodes = true, override_outputs_p = 0.5, allow_io_node_overlap = false)),
      Objective(Forwarding(5, 4), ObstacleAvoidance(0.1, 6), Some(HalfRegionVariation(1, 0.1, reset_region_every_epoch = false)))
    ), "7f794451892a766f76bb10b0b0a87e8b5eda2ec8dbfe0231a2af726ad7fdab91"),
    (Configuration(
      Simulation(ticks_per_seconds = 10, experiment_length = 1200, robot_count = 10, print_analytics = true, Some(5), Some(6)),
      Adaptation(epoch_length = 400, NetworkMutation(0, 1, self_loops = false, 0, 1, keep_p_balance = false), NetworkIOMutation(2, 0.5, 2, 0.5, allow_io_node_overlap = false)),
      Network(n = 100, k = 3, p = 0.79, self_loops = true, NetworkIO(override_output_nodes = true, override_outputs_p = 0.5, allow_io_node_overlap = true)),
      Objective(Forwarding(5, 4), ObstacleAvoidance(0.1, 6), Some(HalfRegionVariation(1, 0.1, reset_region_every_epoch = false)))
    ), "a926ecd7273090d729ea3f59557c62c9da4ddaff6568dc0f397f4e4083125d41"),
    (Configuration(
      Simulation(ticks_per_seconds = 10, experiment_length = 1200, robot_count = 10, print_analytics = true, Some(7), Some(8)),
      Adaptation(epoch_length = 400, NetworkMutation(0, 1, self_loops = false, 0, 1, keep_p_balance = false), NetworkIOMutation(2, 0.5, 2, 0.5, allow_io_node_overlap = false)),
      Network(n = 100, k = 3, p = 0.79, self_loops = true, NetworkIO(override_output_nodes = true, override_outputs_p = 0.5, allow_io_node_overlap = false)),
      Objective(Forwarding(5, 4), ObstacleAvoidance(0.1, 6), Some(HalfRegionVariation(1, 0.1, reset_region_every_epoch = true)))
    ), "18cc55d7bc2fb2496345b097cea2ac4b4170951024c9a8a65213c1c3d9981ebd"),
    (Configuration(
      Simulation(ticks_per_seconds = 10, experiment_length = 1200, robot_count = 12, print_analytics = true, Some(9), Some(10)),
      Adaptation(epoch_length = 400, NetworkMutation(10, 0.5, self_loops = false, 10, 0.5, keep_p_balance = false), NetworkIOMutation(0, 0, 0, 0, allow_io_node_overlap = false)),
      Network(n = 100, k = 3, p = 0.79, self_loops = true, NetworkIO(override_output_nodes = true, override_outputs_p = 0.5, allow_io_node_overlap = false)),
      Objective(Forwarding(5, 4), ObstacleAvoidance(0.1, 6), Some(HalfRegionVariation(1, 0.1, reset_region_every_epoch = false)))
    ), "10825855de61b2cce932676c70fd7c048e94fbb27688011b8498a39ce6818c25"),
    (Configuration(
      Simulation(ticks_per_seconds = 10, experiment_length = 1200, robot_count = 12, print_analytics = true, Some(11), Some(12)),
      Adaptation(epoch_length = 400, NetworkMutation(10, 0.5, self_loops = false, 10, 0.5, keep_p_balance = true), NetworkIOMutation(0, 0, 0, 0, allow_io_node_overlap = false)),
      Network(n = 100, k = 3, p = 0.79, self_loops = true, NetworkIO(override_output_nodes = true, override_outputs_p = 0.5, allow_io_node_overlap = false)),
      Objective(Forwarding(5, 4), ObstacleAvoidance(0.1, 6), Some(HalfRegionVariation(1, 0.1, reset_region_every_epoch = false)))
    ), "41aef79ad7cb58a7292a1eb967d9f2288029d478cac2dd3e64b038daa115a312"),
    (Configuration(
      Simulation(ticks_per_seconds = 10, experiment_length = 1200, robot_count = 12, print_analytics = true, Some(13), Some(14)),
      Adaptation(epoch_length = 400, NetworkMutation(10, 0.5, self_loops = false, 10, 0.5, keep_p_balance = true), NetworkIOMutation(2, 1, 2, 1, allow_io_node_overlap = false)),
      Network(n = 100, k = 3, p = 0.79, self_loops = true, NetworkIO(override_output_nodes = true, override_outputs_p = 0.25, allow_io_node_overlap = false)),
      Objective(Forwarding(5, 4), ObstacleAvoidance(0.1, 6), Some(HalfRegionVariation(1, 0.1, reset_region_every_epoch = false)))
    ), "3e81146b7cea4325c4d26e2562d90b025c16e979f30fbe3ed75d8eab0b11a569"),
    (Configuration(
      Simulation(ticks_per_seconds = 10, experiment_length = 1200, robot_count = 12, print_analytics = true, Some(17), Some(18)),
      Adaptation(epoch_length = 400, NetworkMutation(10, 0.5, self_loops = false, 10, 0.5, keep_p_balance = true), NetworkIOMutation(2, 1, 2, 1, allow_io_node_overlap = false)),
      Network(n = 100, k = 3, p = 0.79, self_loops = true, NetworkIO(override_output_nodes = false, override_outputs_p = 0.25, allow_io_node_overlap = false)),
      Objective(Forwarding(5, 4), ObstacleAvoidance(0.1, 6), Some(HalfRegionVariation(1, 0.1, reset_region_every_epoch = false)))
    ), "764729550527694ac47579ac576065f0f9710e0d8d52c756271b9e4cf620d44e"),
    (Configuration(
      Simulation(ticks_per_seconds = 10, experiment_length = 1200, robot_count = 12, print_analytics = true, Some(19), Some(20)),
      Adaptation(epoch_length = 400, NetworkMutation(10, 0.5, self_loops = false, 10, 0.5, keep_p_balance = true), NetworkIOMutation(2, 1, 2, 1, allow_io_node_overlap = false)),
      Network(n = 100, k = 3, p = 0.79, self_loops = true, NetworkIO(override_output_nodes = true, override_outputs_p = 0.5, allow_io_node_overlap = false)),
      Objective(Forwarding(5, 2), ObstacleAvoidance(0.1, 6), Some(HalfRegionVariation(1, 0.1, reset_region_every_epoch = false)))
    ), "bb70dd09169e696c5dfd5b3995117be75727473520a17c9a28995d5727e48f42"),
    (Configuration(
      Simulation(ticks_per_seconds = 10, experiment_length = 1200, robot_count = 12, print_analytics = true, Some(21), Some(22)),
      Adaptation(epoch_length = 400, NetworkMutation(10, 0.5, self_loops = false, 10, 0.5, keep_p_balance = true), NetworkIOMutation(2, 1, 2, 1, allow_io_node_overlap = false)),
      Network(n = 100, k = 3, p = 0.79, self_loops = true, NetworkIO(override_output_nodes = true, override_outputs_p = 0.5, allow_io_node_overlap = false)),
      Objective(Forwarding(5, 4), ObstacleAvoidance(0.1, 6), Some(HalfRegionVariation(2, 0, reset_region_every_epoch = false)))
    ), "a75d052ab9577d13f02379afeb97afbc44bedc8274293d26e703d808297c24f8"),
    (Configuration(
      Simulation(ticks_per_seconds = 10, experiment_length = 1200, robot_count = 12, print_analytics = true, Some(21), Some(22)),
      Adaptation(epoch_length = 400, NetworkMutation(10, 0.5, self_loops = false, 10, 0.5, keep_p_balance = true), NetworkIOMutation(2, 0.9, 2, 1, allow_io_node_overlap = false)),
      Network(n = 100, k = 3, p = 0.79, self_loops = true, NetworkIO(override_output_nodes = true, override_outputs_p = 0.5, allow_io_node_overlap = false)),
      Objective(Forwarding(5, 4), ObstacleAvoidance(0.1, 6), Some(HalfRegionVariation(2, 0, reset_region_every_epoch = false)))
    ), "2446594f972be9d5c7d80c7f1b6397f18448fe7a3b5a427589804c6dcac2c86c"),
    (Configuration(
      Simulation(ticks_per_seconds = 10, experiment_length = 1200, robot_count = 12, print_analytics = true, Some(21), Some(22)),
      Adaptation(epoch_length = 400, NetworkMutation(10, 0.5, self_loops = false, 10, 0.5, keep_p_balance = true), NetworkIOMutation(2, 1, 2, 0.9, allow_io_node_overlap = false)),
      Network(n = 100, k = 3, p = 0.79, self_loops = true, NetworkIO(override_output_nodes = true, override_outputs_p = 0.5, allow_io_node_overlap = false)),
      Objective(Forwarding(5, 4), ObstacleAvoidance(0.1, 6), Some(HalfRegionVariation(2, 0, reset_region_every_epoch = false)))
    ), "6d7af56dc9849acd02dbdf5434e013faade75deb313b597fcb7520d56211ff90"),
    (Configuration(
      Simulation(ticks_per_seconds = 10, experiment_length = 1200, robot_count = 12, print_analytics = true, Some(21), Some(22)),
      Adaptation(epoch_length = 400, NetworkMutation(10, 0.4, self_loops = false, 10, 0.5, keep_p_balance = true), NetworkIOMutation(2, 1, 2, 1, allow_io_node_overlap = false)),
      Network(n = 100, k = 3, p = 0.79, self_loops = true, NetworkIO(override_output_nodes = true, override_outputs_p = 0.5, allow_io_node_overlap = false)),
      Objective(Forwarding(5, 4), ObstacleAvoidance(0.1, 6), Some(HalfRegionVariation(2, 0, reset_region_every_epoch = false)))
    ), "5400c2f87afe0602f9d4ac03e6752c60d1b0088ca3ca4b6d7dc183b5b3f67e39"),
    (Configuration(
      Simulation(ticks_per_seconds = 10, experiment_length = 1200, robot_count = 12, print_analytics = true, Some(21), Some(22)),
      Adaptation(epoch_length = 400, NetworkMutation(10, 0.5, self_loops = false, 10, 0.4, keep_p_balance = true), NetworkIOMutation(2, 1, 2, 1, allow_io_node_overlap = false)),
      Network(n = 100, k = 3, p = 0.79, self_loops = true, NetworkIO(override_output_nodes = true, override_outputs_p = 0.5, allow_io_node_overlap = false)),
      Objective(Forwarding(5, 4), ObstacleAvoidance(0.1, 6), Some(HalfRegionVariation(2, 0, reset_region_every_epoch = false)))
    ), "2ba441d3a2ec4553a160d99f8257f92324f1c5662fd3bea46afa7524bb0e5a74"),
    (Configuration(
      Simulation(ticks_per_seconds = 10, experiment_length = 1200, robot_count = 12, print_analytics = true, Some(21), Some(22)),
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
        print(result + " == "+ hash + (if(result == hash) " [SUCCESS]" else " [FAILURE]"))
      }
      println(s" ($time)")
  }

}
