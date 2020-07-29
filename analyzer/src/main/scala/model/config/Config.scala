package model.config

import play.api.libs.json.{Json, OFormat}


object Config {

  case class Simulation(ticks_per_seconds: Int, //TODO: seed, robot number
                        experiment_length: Int,
                        network_test_steps: Int,
                        print_analytics: Boolean)

  case class Robot(proximity_threshold: Double,
                   max_wheel_speed: Double)


  object BooleanNetwork {

    case class Options(node_count: Int,
                       nodes_input_count: Int,
                       bias: Double,
                       network_inputs_count: Int,
                       network_outputs_count: Int,
                       self_loops: Boolean,
                       override_output_nodes_bias: Boolean)

  }

  case class BooleanNetwork(max_input_rewires: Int,
                            input_rewires_probability: Double,
                            max_output_rewires: Int,
                            output_rewires_probability: Double,
                            use_dual_encoding: Boolean,
                            options: BooleanNetwork.Options,
                            initial: Option[model.BooleanNetwork.Schema])

  object Implicits {
    implicit def f1: OFormat[Config.Simulation] = Json.format[Config.Simulation]
    implicit def f2: OFormat[Config.Robot] = Json.format[Config.Robot]
    implicit def f3: OFormat[Config.BooleanNetwork.Options] = Json.format[Config.BooleanNetwork.Options]
    implicit def f4: OFormat[Config.BooleanNetwork] = Json.format[Config.BooleanNetwork]
    implicit def f5: OFormat[model.BooleanNetwork.Schema] = Json.format[model.BooleanNetwork.Schema]
    implicit def f6: OFormat[Config] = Json.format[Config]
  }

  def fromJson(json: String): Config = {
    import Implicits._
    Json.fromJson[Config](Json.parse(json)).get
  }
}


case class Config(simulation: Config.Simulation, robot: Config.Robot, bn: Config.BooleanNetwork) {
  def toJson: String = {
    import Config.Implicits._
    Json.toJson(this).toString()
  }
}
