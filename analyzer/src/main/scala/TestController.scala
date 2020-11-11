import model.config.Configuration
import model.config.Configuration._

object TestController extends App {

  implicit val arguments: Array[String] = args

  val configurations: Seq[(Configuration,String)] = Seq(
    (Configuration(
      Simulation(ticks_per_seconds = 10, experiment_length = 1200, robot_count = 10, print_analytics = true, Some(1), Some(2)),
      Adaptation(epoch_length = 400, NetworkMutation(0, 1, 0, 1, keep_p_balance = false), NetworkIOMutation(2, 0.5, 2, 0.5, allow_io_node_overlap = false)),
      Network(n = 100, k = 3, p = 0.79, self_loops = true, NetworkIO(override_output_nodes = true, override_outputs_p = 0.5, allow_io_node_overlap = false)),
      Objective(Forwarding(5, 4), ObstacleAvoidance(0.1, 6), Some(HalfRegionVariation(1, 0.1, reset_region_every_epoch = false)))
    ), "9736b078efe091e7d4d102d3afc1b6dfcdc6798cb1bf2fca3952a37ac9da0c21"),
    (Configuration(
      Simulation(ticks_per_seconds = 10, experiment_length = 1200, robot_count = 10, print_analytics = true, Some(15), Some(16)),
      Adaptation(epoch_length = 400, NetworkMutation(0, 1, 0, 1, keep_p_balance = false), NetworkIOMutation(2, 0.5, 2, 0.5, allow_io_node_overlap = false)),
      Network(n = 100, k = 3, p = 0.79, self_loops = false, NetworkIO(override_output_nodes = true, override_outputs_p = 0.5, allow_io_node_overlap = false)),
      Objective(Forwarding(5, 4), ObstacleAvoidance(0.1, 6), Some(HalfRegionVariation(1, 0.1, reset_region_every_epoch = false)))
    ), "390d9937d9caeb22bbfda74ee0cc107bcd93b019cee0a91c04269f1f8f24adec"),
    (Configuration(
      Simulation(ticks_per_seconds = 10, experiment_length = 1200, robot_count = 10, print_analytics = true, Some(3), Some(4)),
      Adaptation(epoch_length = 400, NetworkMutation(0, 1, 0, 1, keep_p_balance = false), NetworkIOMutation(2, 0.5, 2, 0.5, allow_io_node_overlap = true)),
      Network(n = 100, k = 3, p = 0.79, self_loops = true, NetworkIO(override_output_nodes = true, override_outputs_p = 0.5, allow_io_node_overlap = false)),
      Objective(Forwarding(5, 4), ObstacleAvoidance(0.1, 6), Some(HalfRegionVariation(1, 0.1, reset_region_every_epoch = false)))
    ), "8c13a371b436b35a750134ee12cf00ba442d7cb2b70954162602612d5c2f3c2a"),
    (Configuration(
      Simulation(ticks_per_seconds = 10, experiment_length = 1200, robot_count = 10, print_analytics = true, Some(5), Some(6)),
      Adaptation(epoch_length = 400, NetworkMutation(0, 1, 0, 1, keep_p_balance = false), NetworkIOMutation(2, 0.5, 2, 0.5, allow_io_node_overlap = false)),
      Network(n = 100, k = 3, p = 0.79, self_loops = true, NetworkIO(override_output_nodes = true, override_outputs_p = 0.5, allow_io_node_overlap = true)),
      Objective(Forwarding(5, 4), ObstacleAvoidance(0.1, 6), Some(HalfRegionVariation(1, 0.1, reset_region_every_epoch = false)))
    ), "950f65181e7169455e90daa1c847299c593d5da1d9385cc7c72d245fa094c8a0"),
    (Configuration(
      Simulation(ticks_per_seconds = 10, experiment_length = 1200, robot_count = 10, print_analytics = true, Some(7), Some(8)),
      Adaptation(epoch_length = 400, NetworkMutation(0, 1, 0, 1, keep_p_balance = false), NetworkIOMutation(2, 0.5, 2, 0.5, allow_io_node_overlap = false)),
      Network(n = 100, k = 3, p = 0.79, self_loops = true, NetworkIO(override_output_nodes = true, override_outputs_p = 0.5, allow_io_node_overlap = false)),
      Objective(Forwarding(5, 4), ObstacleAvoidance(0.1, 6), Some(HalfRegionVariation(1, 0.1, reset_region_every_epoch = true)))
    ), "3f56f386c20a8e06b684208bb68e357054d25e52ede2bb56090bdf75c92e6392"),
    (Configuration(
      Simulation(ticks_per_seconds = 10, experiment_length = 1200, robot_count = 12, print_analytics = true, Some(9), Some(10)),
      Adaptation(epoch_length = 400, NetworkMutation(10, 0.5, 10, 0.5, keep_p_balance = false), NetworkIOMutation(0, 0, 0, 0, allow_io_node_overlap = false)),
      Network(n = 100, k = 3, p = 0.79, self_loops = true, NetworkIO(override_output_nodes = true, override_outputs_p = 0.5, allow_io_node_overlap = false)),
      Objective(Forwarding(5, 4), ObstacleAvoidance(0.1, 6), Some(HalfRegionVariation(1, 0.1, reset_region_every_epoch = false)))
    ), "ec79f2d0bf4d4a779d0bf69b384e05020f15658030c85c156ecead0d204284dd"),
    (Configuration(
      Simulation(ticks_per_seconds = 10, experiment_length = 1200, robot_count = 12, print_analytics = true, Some(11), Some(12)),
      Adaptation(epoch_length = 400, NetworkMutation(10, 0.5, 10, 0.5, keep_p_balance = true), NetworkIOMutation(0, 0, 0, 0, allow_io_node_overlap = false)),
      Network(n = 100, k = 3, p = 0.79, self_loops = true, NetworkIO(override_output_nodes = true, override_outputs_p = 0.5, allow_io_node_overlap = false)),
      Objective(Forwarding(5, 4), ObstacleAvoidance(0.1, 6), Some(HalfRegionVariation(1, 0.1, reset_region_every_epoch = false)))
    ), "b25f446f3eda1a329bba9b665251ee4649e350309d4391daaf817ce907f0e5e1"),
    (Configuration(
      Simulation(ticks_per_seconds = 10, experiment_length = 1200, robot_count = 12, print_analytics = true, Some(13), Some(14)),
      Adaptation(epoch_length = 400, NetworkMutation(10, 0.5, 10, 0.5, keep_p_balance = true), NetworkIOMutation(2, 1, 2, 1, allow_io_node_overlap = false)),
      Network(n = 100, k = 3, p = 0.79, self_loops = true, NetworkIO(override_output_nodes = true, override_outputs_p = 0.25, allow_io_node_overlap = false)),
      Objective(Forwarding(5, 4), ObstacleAvoidance(0.1, 6), Some(HalfRegionVariation(1, 0.1, reset_region_every_epoch = false)))
    ), "3831d46fb7e4bcb6d89be9dfcf8e9f1b0a6691c48858aff6e5a3d25b76b82119"),
    (Configuration(
      Simulation(ticks_per_seconds = 10, experiment_length = 1200, robot_count = 12, print_analytics = true, Some(17), Some(18)),
      Adaptation(epoch_length = 400, NetworkMutation(10, 0.5, 10, 0.5, keep_p_balance = true), NetworkIOMutation(2, 1, 2, 1, allow_io_node_overlap = false)),
      Network(n = 100, k = 3, p = 0.79, self_loops = true, NetworkIO(override_output_nodes = false, override_outputs_p = 0.25, allow_io_node_overlap = false)),
      Objective(Forwarding(5, 4), ObstacleAvoidance(0.1, 6), Some(HalfRegionVariation(1, 0.1, reset_region_every_epoch = false)))
    ), "aef2ee5b6c15683c1d1e6779d33c1ca0e02b4437c35f73903daa44f6987153f7"),
    (Configuration(
      Simulation(ticks_per_seconds = 10, experiment_length = 1200, robot_count = 12, print_analytics = true, Some(19), Some(20)),
      Adaptation(epoch_length = 400, NetworkMutation(10, 0.5, 10, 0.5, keep_p_balance = true), NetworkIOMutation(2, 1, 2, 1, allow_io_node_overlap = false)),
      Network(n = 100, k = 3, p = 0.79, self_loops = true, NetworkIO(override_output_nodes = true, override_outputs_p = 0.5, allow_io_node_overlap = false)),
      Objective(Forwarding(5, 2), ObstacleAvoidance(0.1, 6), Some(HalfRegionVariation(1, 0.1, reset_region_every_epoch = false)))
    ), "3464c8edd35e13ffa51811bf1753869e4a6a11f4d100c1e5285958e8224c2beb"),
    (Configuration(
      Simulation(ticks_per_seconds = 10, experiment_length = 1200, robot_count = 12, print_analytics = true, Some(21), Some(22)),
      Adaptation(epoch_length = 400, NetworkMutation(10, 0.5, 10, 0.5, keep_p_balance = true), NetworkIOMutation(2, 1, 2, 1, allow_io_node_overlap = false)),
      Network(n = 100, k = 3, p = 0.79, self_loops = true, NetworkIO(override_output_nodes = true, override_outputs_p = 0.5, allow_io_node_overlap = false)),
      Objective(Forwarding(5, 4), ObstacleAvoidance(0.1, 6), Some(HalfRegionVariation(2, 0, reset_region_every_epoch = false)))
    ), "9ccf4838d3d65e938ed55125dd9d5fd95bfaf8a266d4f771df056aa3c057ca9f"),
    (Configuration(
      Simulation(ticks_per_seconds = 10, experiment_length = 1200, robot_count = 12, print_analytics = true, Some(21), Some(22)),
      Adaptation(epoch_length = 400, NetworkMutation(10, 0.5, 10, 0.5, keep_p_balance = true), NetworkIOMutation(2, 0.9, 2, 1, allow_io_node_overlap = false)),
      Network(n = 100, k = 3, p = 0.79, self_loops = true, NetworkIO(override_output_nodes = true, override_outputs_p = 0.5, allow_io_node_overlap = false)),
      Objective(Forwarding(5, 4), ObstacleAvoidance(0.1, 6), Some(HalfRegionVariation(2, 0, reset_region_every_epoch = false)))
    ), "75026480683d44e21ec3c59d88435ee48b3986cdc9b96fcb129c73310ebb79e0"),
    (Configuration(
      Simulation(ticks_per_seconds = 10, experiment_length = 1200, robot_count = 12, print_analytics = true, Some(21), Some(22)),
      Adaptation(epoch_length = 400, NetworkMutation(10, 0.5, 10, 0.5, keep_p_balance = true), NetworkIOMutation(2, 1, 2, 0.9, allow_io_node_overlap = false)),
      Network(n = 100, k = 3, p = 0.79, self_loops = true, NetworkIO(override_output_nodes = true, override_outputs_p = 0.5, allow_io_node_overlap = false)),
      Objective(Forwarding(5, 4), ObstacleAvoidance(0.1, 6), Some(HalfRegionVariation(2, 0, reset_region_every_epoch = false)))
    ), "bfa20318ced88df23be8ebf9c997254576a7918c6f8cfaa8892427d41196488b"),
    (Configuration(
      Simulation(ticks_per_seconds = 10, experiment_length = 1200, robot_count = 12, print_analytics = true, Some(21), Some(22)),
      Adaptation(epoch_length = 400, NetworkMutation(10, 0.4, 10, 0.5, keep_p_balance = true), NetworkIOMutation(2, 1, 2, 1, allow_io_node_overlap = false)),
      Network(n = 100, k = 3, p = 0.79, self_loops = true, NetworkIO(override_output_nodes = true, override_outputs_p = 0.5, allow_io_node_overlap = false)),
      Objective(Forwarding(5, 4), ObstacleAvoidance(0.1, 6), Some(HalfRegionVariation(2, 0, reset_region_every_epoch = false)))
    ), "df1a6fcee0d2f7efb8db88f5fd951118cd84b5dc0edcf6c4deeadb80f82f59a0"),
    (Configuration(
      Simulation(ticks_per_seconds = 10, experiment_length = 1200, robot_count = 12, print_analytics = true, Some(21), Some(22)),
      Adaptation(epoch_length = 400, NetworkMutation(10, 0.5, 10, 0.4, keep_p_balance = true), NetworkIOMutation(2, 1, 2, 1, allow_io_node_overlap = false)),
      Network(n = 100, k = 3, p = 0.79, self_loops = true, NetworkIO(override_output_nodes = true, override_outputs_p = 0.5, allow_io_node_overlap = false)),
      Objective(Forwarding(5, 4), ObstacleAvoidance(0.1, 6), Some(HalfRegionVariation(2, 0, reset_region_every_epoch = false)))
    ), "8da1a3e9b5afb0368dc5a969bcf6a48143a4b8c0cdcab5abc673b6ef7aa20d8e")
  )

  configurations.drop(0).zipWithIndex.foreach {
    case ((config, hash), i) =>
      print(s"Executing test $i... ")
      val exp1 = Experiments.runSimulation(config, visualization = false).filter(_.headOption.contains('{'))
      val result = utils.Hash.sha256(exp1)
      println(result + " == "+ hash + (if(result == hash) " [SUCCESS]" else " [FAILURE]"))
  }

}
