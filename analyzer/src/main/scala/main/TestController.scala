package main

import model.config.Configuration
import model.config.Configuration._
import monocle.Lens
import utils.ConfigLens.lens
import utils.ConfigLens._
import utils.Parallel._

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
    Test("7e65897ef735803c223d5cbf7d0cbd5f62fff328e43eb81856d29984eff57a65", crLens, (1.0, 20)),
    Test("ca42200dcd71a6fd5d477ea88052030b62e6c91ce74ddd02123bcbade1934657", lens(_.adaptation.network_mutation.connection_rewire_probability), 1.0),
    Test("ca42200dcd71a6fd5d477ea88052030b62e6c91ce74ddd02123bcbade1934657", lens(_.adaptation.network_mutation.function_bit_flips_probability), 1.0),
    Test("3f850141596d0ff826b4648762e238e3d6632b713b15bc12d9b63e0fc701f4d5", lens(_.network.io.override_outputs_p), 0.25),
    Test("ca42200dcd71a6fd5d477ea88052030b62e6c91ce74ddd02123bcbade1934657", lens(_.network.io.override_outputs_p), 0.5),
    Test("ca42200dcd71a6fd5d477ea88052030b62e6c91ce74ddd02123bcbade1934657", lens(_.network.io.override_output_nodes), true),
    Test("3c2038751519debef954966b770bf8c6012201aebd77bbbcdc946031bcff4ef0", lens(_.network.io.override_output_nodes), false),
    Test("451b5f8cd083b7a45e0649768293e8bc9a7dc4c4f238d46d890097ba69c9dc57", lens(_.network.io.allow_io_node_overlap), true),
    Test("ca42200dcd71a6fd5d477ea88052030b62e6c91ce74ddd02123bcbade1934657", lens(_.network.io.allow_io_node_overlap), false),
    Test("ca42200dcd71a6fd5d477ea88052030b62e6c91ce74ddd02123bcbade1934657", lens(_.network.self_loops), true),
    Test("69eddb683af2ce8a38c2d9c5f70c557c93d3f6b2cdaa85d76750466662f40a67", lens(_.network.self_loops), false),
    Test("3dbf23f6c2a1a5ca014ce7c368ac1f6f5f98e4b4f36ff4cddd9a855aa1d5e51c", lens(_.network.k), 2),
    Test("8722321e322bfeccb8f398ec010a246d9ebe83dc367afbc498fca7b39a5c2c40", lens(_.network.k), 4),
    Test("ca42200dcd71a6fd5d477ea88052030b62e6c91ce74ddd02123bcbade1934657", lens(_.network.k), 3),
    Test("bde443f77b8c9bcfe93d4219d3396fe536dff1c84b7c669fd62c9244c6ae74c2", lens(_.network.n), 50),
    Test("d7e17b8cb1233b94f515f6bf8fe598c1f37d1ebe8a644e4bc204b70e2cf68bf7", lens(_.network.n), 150),
    Test("ca42200dcd71a6fd5d477ea88052030b62e6c91ce74ddd02123bcbade1934657", lens(_.network.n), 100),
    Test("7bbbbd0e5693470bb9ee6f310d2fe74d3600283c353410d981cf9423cf26404e", lens(_.network.p), 0.1),
    Test("b3c120a644be8ea5b839963b0aa85d38dc668b75ade8a6b7c1bd271332f816a7", lens(_.network.p), 0.5),
    Test("ca42200dcd71a6fd5d477ea88052030b62e6c91ce74ddd02123bcbade1934657", lens(_.network.p), 0.79),
    Test("7e6d4e4c4726de112a9ddd32da47698f9e85819a14e3d26bb105350782f38400", orLens and rOverlapLens, ((0.5, 2), true)),
    Test("36a2d5de39adf81535f1e7e8405b13f56169aaf4516b338a9b01e2165aa79fc0", irLens and rOverlapLens, ((0.5, 2), true)),
    Test("ca42200dcd71a6fd5d477ea88052030b62e6c91ce74ddd02123bcbade1934657", irLens, (1.0, 0)),
    Test("ca42200dcd71a6fd5d477ea88052030b62e6c91ce74ddd02123bcbade1934657", irLens, (0.0, 0)),
    Test("7927df517d35def859b2910072877008928df232f8043699bedde40d907ce0ec", irLens, (0.6, 2)),
    Test("cd80ee5616d0a2e7af81c9b39bb55354fb4b172a544b0d6f80f647e832a50c1f", irLens, (0.5, 2)),
    Test("ca42200dcd71a6fd5d477ea88052030b62e6c91ce74ddd02123bcbade1934657", orLens, (1.0, 0)),
    Test("ca42200dcd71a6fd5d477ea88052030b62e6c91ce74ddd02123bcbade1934657", orLens, (0.0, 0)),
    Test("5c3a24b929ac379d1245afbea293fbb11d71e6e1a6cd0f25a69dcedad28d3960", orLens, (0.6, 2)),
    Test("1256d7993d719254849477ab599b16b39365d1587eed1f934286e1cbd4ea4cf7", orLens, (0.5, 2)),
  )
  tests.parForeach(Args.PARALLELISM_DEGREE, _.run())
}
