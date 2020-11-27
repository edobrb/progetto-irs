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
    Test("492f13b15f3b9fe5866d8b5ac6bea83dd48aae2b644622b50ee6980a5503d3a3", crLens, (1.0, 20)),
    Test("818eadd909b9757372c4518bf3376cbb2b4b3e7aaa91c07bf0e0d8e44a8ce0c2", lens(_.adaptation.network_mutation.connection_rewire_probability), 1.0),
    Test("818eadd909b9757372c4518bf3376cbb2b4b3e7aaa91c07bf0e0d8e44a8ce0c2", lens(_.adaptation.network_mutation.function_bit_flips_probability), 1.0),
    Test("3f850141596d0ff826b4648762e238e3d6632b713b15bc12d9b63e0fc701f4d5", lens(_.network.io.override_outputs_p), 0.25),
    Test("818eadd909b9757372c4518bf3376cbb2b4b3e7aaa91c07bf0e0d8e44a8ce0c2", lens(_.network.io.override_outputs_p), 0.5),
    Test("818eadd909b9757372c4518bf3376cbb2b4b3e7aaa91c07bf0e0d8e44a8ce0c2", lens(_.network.io.override_output_nodes), true),
    Test("3c2038751519debef954966b770bf8c6012201aebd77bbbcdc946031bcff4ef0", lens(_.network.io.override_output_nodes), false),
    Test("624f23b17a66b914b7cdee24eb202f47e77678ad92be120a45208f969b3a96ce", lens(_.network.io.allow_io_node_overlap), true),
    Test("818eadd909b9757372c4518bf3376cbb2b4b3e7aaa91c07bf0e0d8e44a8ce0c2", lens(_.network.io.allow_io_node_overlap), false),
    Test("818eadd909b9757372c4518bf3376cbb2b4b3e7aaa91c07bf0e0d8e44a8ce0c2", lens(_.network.self_loops), true),
    Test("d5a232a74f8527446e586a25b2179f6bd294983a1e2336c8f634a0f28c2e6fbb", lens(_.network.self_loops), false),
    Test("9442f616f5b1548b9010090b9d28718dcb1779e48b05955084211d761b43729", lens(_.network.k), 2),
    Test("d7f2002d051e52f2b89383bf6c1f14ffe74959168b83491acb924889a6dfb4e0", lens(_.network.k), 4),
    Test("818eadd909b9757372c4518bf3376cbb2b4b3e7aaa91c07bf0e0d8e44a8ce0c2", lens(_.network.k), 3),
    Test("bde443f77b8c9bcfe93d4219d3396fe536dff1c84b7c669fd62c9244c6ae74c2", lens(_.network.n), 50),
    Test("2e6738019a285dfb2cbf26bf38b91f7cf5a3a307449cf1623563b8e0db1cdc0", lens(_.network.n), 150),
    Test("818eadd909b9757372c4518bf3376cbb2b4b3e7aaa91c07bf0e0d8e44a8ce0c2", lens(_.network.n), 100),
    Test("d3e249419d105bcfeeb2c58b5f44033238fbc760f7f8f38e12b6f546679e154e", lens(_.network.p), 0.1),
    Test("f692484a23a981aef208e5ad8158df3acfd832c1523f176e95f7f0dac65ce151", lens(_.network.p), 0.5),
    Test("818eadd909b9757372c4518bf3376cbb2b4b3e7aaa91c07bf0e0d8e44a8ce0c2", lens(_.network.p), 0.79),
    Test("1f01beb7eb2ce14f1c468ac805c7e42071ffb0850309383b5ef405ba5454f878", orLens and rOverlapLens, ((0.5, 2), true)),
    Test("fc8d6b6f7c185aa31a34015929eac50ac1ac7682cd3f457d64dad99fe73359bf", irLens and rOverlapLens, ((0.5, 2), true)),
    Test("818eadd909b9757372c4518bf3376cbb2b4b3e7aaa91c07bf0e0d8e44a8ce0c2", irLens, (1.0, 0)),
    Test("818eadd909b9757372c4518bf3376cbb2b4b3e7aaa91c07bf0e0d8e44a8ce0c2", irLens, (0.0, 0)),
    Test("1e180aa893239cc9022e1b9247343f455fffd0dcf7dc63d8993f36cdc9a54653", irLens, (0.6, 2)),
    Test("548a7db9c0cd3e76c74eccf479803ee6668d57d7997e73275dbabc8f75795ef9", irLens, (0.5, 2)),
    Test("818eadd909b9757372c4518bf3376cbb2b4b3e7aaa91c07bf0e0d8e44a8ce0c2", orLens, (1.0, 0)),
    Test("818eadd909b9757372c4518bf3376cbb2b4b3e7aaa91c07bf0e0d8e44a8ce0c2", orLens, (0.0, 0)),
    Test("e76e00d95c22ad9cf7be23a3600093826f742bf5db02f2456efb44c6c2dc086d", orLens, (0.6, 2)),
    Test("54b70fe20e8ca11710f611ac26b3a053cfa8389d2fa6319249167659f3dc4df1", orLens, (0.5, 2)),
  )
  tests.parForeach(Args.PARALLELISM_DEGREE, _.run())
}
