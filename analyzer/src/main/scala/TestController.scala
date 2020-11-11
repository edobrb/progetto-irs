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
    ), "dc640545b118f52da240cda041f2e2c5d57ed26b73f5049c724472905bfdf7ae"),
    (Configuration(
      Simulation(ticks_per_seconds = 10, experiment_length = 1200, robot_count = 10, print_analytics = true, Some(15), Some(16)),
      Adaptation(epoch_length = 400, NetworkMutation(0, 1, 0, 1, keep_p_balance = false), NetworkIOMutation(2, 0.5, 2, 0.5, allow_io_node_overlap = false)),
      Network(n = 100, k = 3, p = 0.79, self_loops = false, NetworkIO(override_output_nodes = true, override_outputs_p = 0.5, allow_io_node_overlap = false)),
      Objective(Forwarding(5, 4), ObstacleAvoidance(0.1, 6), Some(HalfRegionVariation(1, 0.1, reset_region_every_epoch = false)))
    ), "576a24df0d5b7178d48399c325aaf7612176fad8f1db55975e86475772c3459d"),
    (Configuration(
      Simulation(ticks_per_seconds = 10, experiment_length = 1200, robot_count = 10, print_analytics = true, Some(3), Some(4)),
      Adaptation(epoch_length = 400, NetworkMutation(0, 1, 0, 1, keep_p_balance = false), NetworkIOMutation(2, 0.5, 2, 0.5, allow_io_node_overlap = true)),
      Network(n = 100, k = 3, p = 0.79, self_loops = true, NetworkIO(override_output_nodes = true, override_outputs_p = 0.5, allow_io_node_overlap = false)),
      Objective(Forwarding(5, 4), ObstacleAvoidance(0.1, 6), Some(HalfRegionVariation(1, 0.1, reset_region_every_epoch = false)))
    ), "46465a4004dff9bf07594b3e39093c71af2cbaa4caad3f80c6484862dc1dfc76"),
    (Configuration(
      Simulation(ticks_per_seconds = 10, experiment_length = 1200, robot_count = 10, print_analytics = true, Some(5), Some(6)),
      Adaptation(epoch_length = 400, NetworkMutation(0, 1, 0, 1, keep_p_balance = false), NetworkIOMutation(2, 0.5, 2, 0.5, allow_io_node_overlap = false)),
      Network(n = 100, k = 3, p = 0.79, self_loops = true, NetworkIO(override_output_nodes = true, override_outputs_p = 0.5, allow_io_node_overlap = true)),
      Objective(Forwarding(5, 4), ObstacleAvoidance(0.1, 6), Some(HalfRegionVariation(1, 0.1, reset_region_every_epoch = false)))
    ), "ee911933bf251a738c609735fb8877d87a50679e9cc70c03c6897ab20bd44dbc"),
    (Configuration(
      Simulation(ticks_per_seconds = 10, experiment_length = 1200, robot_count = 10, print_analytics = true, Some(7), Some(8)),
      Adaptation(epoch_length = 400, NetworkMutation(0, 1, 0, 1, keep_p_balance = false), NetworkIOMutation(2, 0.5, 2, 0.5, allow_io_node_overlap = false)),
      Network(n = 100, k = 3, p = 0.79, self_loops = true, NetworkIO(override_output_nodes = true, override_outputs_p = 0.5, allow_io_node_overlap = false)),
      Objective(Forwarding(5, 4), ObstacleAvoidance(0.1, 6), Some(HalfRegionVariation(1, 0.1, reset_region_every_epoch = true)))
    ), "d513e68538d41b2bb33e3821ca99d624c9ce58a71681bef2ce558ebd607882a5"),
    (Configuration(
      Simulation(ticks_per_seconds = 10, experiment_length = 1200, robot_count = 12, print_analytics = true, Some(9), Some(10)),
      Adaptation(epoch_length = 400, NetworkMutation(10, 0.5, 10, 0.5, keep_p_balance = false), NetworkIOMutation(0, 0, 0, 0, allow_io_node_overlap = false)),
      Network(n = 100, k = 3, p = 0.79, self_loops = true, NetworkIO(override_output_nodes = true, override_outputs_p = 0.5, allow_io_node_overlap = false)),
      Objective(Forwarding(5, 4), ObstacleAvoidance(0.1, 6), Some(HalfRegionVariation(1, 0.1, reset_region_every_epoch = false)))
    ), "eab5d9625014a307357fcb2bffcf97fa8ab94691a3883276411ad99325f1ced8"),
    (Configuration(
      Simulation(ticks_per_seconds = 10, experiment_length = 1200, robot_count = 12, print_analytics = true, Some(11), Some(12)),
      Adaptation(epoch_length = 400, NetworkMutation(10, 0.5, 10, 0.5, keep_p_balance = true), NetworkIOMutation(0, 0, 0, 0, allow_io_node_overlap = false)),
      Network(n = 100, k = 3, p = 0.79, self_loops = true, NetworkIO(override_output_nodes = true, override_outputs_p = 0.5, allow_io_node_overlap = false)),
      Objective(Forwarding(5, 4), ObstacleAvoidance(0.1, 6), Some(HalfRegionVariation(1, 0.1, reset_region_every_epoch = false)))
    ), "bb3ca09152379e3fbaad0c9b8218143684b3bfa6f633244fdb7114542eb6d71e"),
    (Configuration(
      Simulation(ticks_per_seconds = 10, experiment_length = 1200, robot_count = 12, print_analytics = true, Some(13), Some(14)),
      Adaptation(epoch_length = 400, NetworkMutation(10, 0.5, 10, 0.5, keep_p_balance = true), NetworkIOMutation(2, 1, 2, 1, allow_io_node_overlap = false)),
      Network(n = 100, k = 3, p = 0.79, self_loops = true, NetworkIO(override_output_nodes = true, override_outputs_p = 0.25, allow_io_node_overlap = false)),
      Objective(Forwarding(5, 4), ObstacleAvoidance(0.1, 6), Some(HalfRegionVariation(1, 0.1, reset_region_every_epoch = false)))
    ), "ac471253a02be2900ad4a653029c75b16f9823392c97ac65989b1a655cdd9fe9"),
    (Configuration(
      Simulation(ticks_per_seconds = 10, experiment_length = 1200, robot_count = 12, print_analytics = true, Some(17), Some(18)),
      Adaptation(epoch_length = 400, NetworkMutation(10, 0.5, 10, 0.5, keep_p_balance = true), NetworkIOMutation(2, 1, 2, 1, allow_io_node_overlap = false)),
      Network(n = 100, k = 3, p = 0.79, self_loops = true, NetworkIO(override_output_nodes = false, override_outputs_p = 0.25, allow_io_node_overlap = false)),
      Objective(Forwarding(5, 4), ObstacleAvoidance(0.1, 6), Some(HalfRegionVariation(1, 0.1, reset_region_every_epoch = false)))
    ), "9f6c6ec928d117e01fb14bff886b0922049c33b7b59ee84d49962956e5052b2f"),
    (Configuration(
      Simulation(ticks_per_seconds = 10, experiment_length = 1200, robot_count = 12, print_analytics = true, Some(19), Some(20)),
      Adaptation(epoch_length = 400, NetworkMutation(10, 0.5, 10, 0.5, keep_p_balance = true), NetworkIOMutation(2, 1, 2, 1, allow_io_node_overlap = false)),
      Network(n = 100, k = 3, p = 0.79, self_loops = true, NetworkIO(override_output_nodes = true, override_outputs_p = 0.5, allow_io_node_overlap = false)),
      Objective(Forwarding(5, 2), ObstacleAvoidance(0.1, 6), Some(HalfRegionVariation(1, 0.1, reset_region_every_epoch = false)))
    ), "e9c672e1d29b6a060fd2542471eb1e14ff3240f50dc1d1656f00d03350a69bf1"),
    (Configuration(
      Simulation(ticks_per_seconds = 10, experiment_length = 1200, robot_count = 12, print_analytics = true, Some(21), Some(22)),
      Adaptation(epoch_length = 400, NetworkMutation(10, 0.5, 10, 0.5, keep_p_balance = true), NetworkIOMutation(2, 1, 2, 1, allow_io_node_overlap = false)),
      Network(n = 100, k = 3, p = 0.79, self_loops = true, NetworkIO(override_output_nodes = true, override_outputs_p = 0.5, allow_io_node_overlap = false)),
      Objective(Forwarding(5, 4), ObstacleAvoidance(0.1, 6), Some(HalfRegionVariation(2, 0, reset_region_every_epoch = false)))
    ), "99f92f4c16e2bb948f1d1ee6948bee107b18ff8ac311d407aaf50e3244bd29f6"),
    (Configuration(
      Simulation(ticks_per_seconds = 10, experiment_length = 1200, robot_count = 12, print_analytics = true, Some(21), Some(22)),
      Adaptation(epoch_length = 400, NetworkMutation(10, 0.5, 10, 0.5, keep_p_balance = true), NetworkIOMutation(2, 0.9, 2, 1, allow_io_node_overlap = false)),
      Network(n = 100, k = 3, p = 0.79, self_loops = true, NetworkIO(override_output_nodes = true, override_outputs_p = 0.5, allow_io_node_overlap = false)),
      Objective(Forwarding(5, 4), ObstacleAvoidance(0.1, 6), Some(HalfRegionVariation(2, 0, reset_region_every_epoch = false)))
    ), "6c03c9d1213862324c8ab6ff72f742fa3fb85895fe2203931a482b9a12c9dce5"),
    (Configuration(
      Simulation(ticks_per_seconds = 10, experiment_length = 1200, robot_count = 12, print_analytics = true, Some(21), Some(22)),
      Adaptation(epoch_length = 400, NetworkMutation(10, 0.5, 10, 0.5, keep_p_balance = true), NetworkIOMutation(2, 1, 2, 0.9, allow_io_node_overlap = false)),
      Network(n = 100, k = 3, p = 0.79, self_loops = true, NetworkIO(override_output_nodes = true, override_outputs_p = 0.5, allow_io_node_overlap = false)),
      Objective(Forwarding(5, 4), ObstacleAvoidance(0.1, 6), Some(HalfRegionVariation(2, 0, reset_region_every_epoch = false)))
    ), "5872f7222c22044c90f9c57f71dcb9e34b924453a5c77a8433d2eb2431daf33a"),
    (Configuration(
      Simulation(ticks_per_seconds = 10, experiment_length = 1200, robot_count = 12, print_analytics = true, Some(21), Some(22)),
      Adaptation(epoch_length = 400, NetworkMutation(10, 0.4, 10, 0.5, keep_p_balance = true), NetworkIOMutation(2, 1, 2, 1, allow_io_node_overlap = false)),
      Network(n = 100, k = 3, p = 0.79, self_loops = true, NetworkIO(override_output_nodes = true, override_outputs_p = 0.5, allow_io_node_overlap = false)),
      Objective(Forwarding(5, 4), ObstacleAvoidance(0.1, 6), Some(HalfRegionVariation(2, 0, reset_region_every_epoch = false)))
    ), "b0b315332ccb911548b9277db9cf49e96176aa1a31874d84a19abea70b672c68"),
    (Configuration(
      Simulation(ticks_per_seconds = 10, experiment_length = 1200, robot_count = 12, print_analytics = true, Some(21), Some(22)),
      Adaptation(epoch_length = 400, NetworkMutation(10, 0.5, 10, 0.4, keep_p_balance = true), NetworkIOMutation(2, 1, 2, 1, allow_io_node_overlap = false)),
      Network(n = 100, k = 3, p = 0.79, self_loops = true, NetworkIO(override_output_nodes = true, override_outputs_p = 0.5, allow_io_node_overlap = false)),
      Objective(Forwarding(5, 4), ObstacleAvoidance(0.1, 6), Some(HalfRegionVariation(2, 0, reset_region_every_epoch = false)))
    ), "5f8ba57b0d084f0de4b32289828303ffdf37ed897ae0bc8182c8e15a85e4a333")
  )

  configurations.drop(0).zipWithIndex.foreach {
    case ((config, hash), i) =>
      print(s"Executing test $i... ")
      val exp1 = Experiments.runSimulation(config, visualization = false).filter(_.headOption.contains('{'))
      val result = utils.Hash.sha256(exp1)
      println(result + " == "+ hash + (if(result == hash) " [SUCCESS]" else " [FAILURE]"))
  }

}
