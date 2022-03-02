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
  val otherLens = lens(_.other)
  val tests = Seq(
    Test("a2505fa36aa6c70a2b3cf3b33e868e7a795a57d3cd0b44cf208dec0630b8d010", otherLens and irLens, (Map("target_entropy" -> "1.7", "combined_fitness_entropy" -> "", "alpha" -> "3.0", "beta" -> "0.75"), (1.0, 8))),
    Test("a69e78fc61b68fd8e4db01703afb8e095d00e7f299668ece971772be6f3bfa53", otherLens and irLens, (Map("target_entropy" -> "1.5"), (1.0, 8))),
    Test("1261a116fe1be48827ec4eccaff1f80fcb27f12a456313db8b5ab13d7b405040", otherLens and irLens, (Map("target_entropy" -> "5"), (1.0, 8))),
    Test("2794c498f0a7575f4d5716bb389be455628b2197815692105b7c7730821f8e3d", crLens, (1.0, 20)),
    Test("9f49b7e2bab8d0ae35f034eabe05890af2ad2155f334db8d72ad1d2e50f32264", lens(_.adaptation.network_mutation.connection_rewire_probability), 1.0),
    Test("9f49b7e2bab8d0ae35f034eabe05890af2ad2155f334db8d72ad1d2e50f32264", lens(_.adaptation.network_mutation.function_bit_flips_probability), 1.0),
    Test("82d735d56616ae128e49b9b8e3ba213dee0850e5b8902b86eec1502dceb1ed90", lens(_.network.io.override_outputs_p), 0.25),
    Test("9f49b7e2bab8d0ae35f034eabe05890af2ad2155f334db8d72ad1d2e50f32264", lens(_.network.io.override_outputs_p), 0.5),
    Test("9f49b7e2bab8d0ae35f034eabe05890af2ad2155f334db8d72ad1d2e50f32264", lens(_.network.io.override_output_nodes), true),
    Test("824c6af8a74bd6a58448c3da070aaad4818dcdfd5732c0ca90f19dd8503634c7", lens(_.network.io.override_output_nodes), false),
    Test("3a20786a28ebeb6ce86fc42b6daa67fb63db27e82e20009c91d553cd40552007", lens(_.network.io.allow_io_node_overlap), true),
    Test("9f49b7e2bab8d0ae35f034eabe05890af2ad2155f334db8d72ad1d2e50f32264", lens(_.network.io.allow_io_node_overlap), false),
    Test("9f49b7e2bab8d0ae35f034eabe05890af2ad2155f334db8d72ad1d2e50f32264", lens(_.network.self_loops), true),
    Test("120cd8593243a1e59816b834ab14ecc95d8a337bb4343b873e5ed18cf192557a", lens(_.network.self_loops), false),
    Test("cb2f28c9f5776dc6e4ae27ac1bc4f0849f17902a9affc01abbad7905771d5cdc", lens(_.network.k), 2),
    Test("f215dad86325cc429e268953d88df7c562939e9b196791f3bf098dd2487211ec", lens(_.network.k), 4),
    Test("9f49b7e2bab8d0ae35f034eabe05890af2ad2155f334db8d72ad1d2e50f32264", lens(_.network.k), 3),
    Test("7e2dbe96eeee048d23e5832ba04660323dc2167b17069da026bee9ed135c4323", lens(_.network.n), 50),
    Test("f1ceab35849361aa17e099a3546a8cd7bfc2362dd093dabdcfcc2fe63e04c8d", lens(_.network.n), 150),
    Test("9f49b7e2bab8d0ae35f034eabe05890af2ad2155f334db8d72ad1d2e50f32264", lens(_.network.n), 100),
    Test("6c03951fe064d6c1309735b5726f3d42974b766b2e881dcf9b91d12d8adab64", lens(_.network.p), 0.1),
    Test("e6ea44baac7cba5af1cfd439a0d10330b19f41b4263f5991ba439ab543b95ec7", lens(_.network.p), 0.5),
    Test("9f49b7e2bab8d0ae35f034eabe05890af2ad2155f334db8d72ad1d2e50f32264", lens(_.network.p), 0.79),
    Test("6b193775ef927b086d9709a7f778801d4184889bc3d213259f2144d787066a62", orLens and rOverlapLens, ((0.5, 2), true)),
    Test("b1e9935beeb8a39426fe461ac4b74ade2b068b14221ab26129390efc06570363", irLens and rOverlapLens, ((0.5, 2), true)),
    Test("9f49b7e2bab8d0ae35f034eabe05890af2ad2155f334db8d72ad1d2e50f32264", irLens, (1.0, 0)),
    Test("9f49b7e2bab8d0ae35f034eabe05890af2ad2155f334db8d72ad1d2e50f32264", irLens, (0.0, 0)),
    Test("75706aeffd0a03503340530ebef466ac8e4838d3109e0168e58b57719aa70d61", irLens, (0.6, 2)),
    Test("9d0da398590b771faeebe39bed0e2d0b4e344f758a9f0da27889aaedee3229b2", irLens, (0.5, 2)),
    Test("9f49b7e2bab8d0ae35f034eabe05890af2ad2155f334db8d72ad1d2e50f32264", orLens, (1.0, 0)),
    Test("9f49b7e2bab8d0ae35f034eabe05890af2ad2155f334db8d72ad1d2e50f32264", orLens, (0.0, 0)),
    Test("61b6457cb128aa6f2a82d48608a75e68ab803f2de08b651ada9dd329522a6724", orLens, (0.6, 2)),
    Test("edba6815467facf3b1506c5701d66efe63486639c4fc0ae77d9b8c9b3a21d3ff", orLens, (0.5, 2)),
  )
  tests.parForeach(Args.PARALLELISM_DEGREE, _.run())
}
