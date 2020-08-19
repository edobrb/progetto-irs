package model.config

import play.api.libs.json.{Json, OFormat}
import play.api.libs.json._       // JSON library
import play.api.libs.json.Reads._

object Config {

  case class Simulation(ticks_per_seconds: Int, //TODO: seed
                        experiment_length: Int,
                        network_test_steps: Int,
                        override_robot_count: Option[Int],
                        print_analytics: Boolean) {
    def robot_count: Int = override_robot_count.getOrElse(10)
  }

  case class Robot(proximity_threshold: Double,
                   max_wheel_speed: Double,
                   stay_on_half:Boolean)


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

  object JsonFormats {


    implicit def f1: OFormat[Config.Simulation] = Json.format[Config.Simulation]

    implicit def f2: OFormat[Config.Robot] = new OFormat[Config.Robot] {
      override def reads(json: JsValue): JsResult[Robot] = json match {
        case JsObject(obj) =>
          (obj.get("proximity_threshold"), obj.get("max_wheel_speed"), obj.get("stay_on_half")) match {
            case (Some(JsNumber(pt)), Some(JsNumber(mws)), None) => JsSuccess(Robot(pt.toDouble, mws.toDouble, stay_on_half = false))
            case (Some(JsNumber(pt)), Some(JsNumber(mws)), Some(JsBoolean(soh))) => JsSuccess(Robot(pt.toDouble, mws.toDouble, soh))
            case _ => JsError()
          }
        case _ => JsError()
      }

      override def writes(o: Robot): JsObject = JsObject(Seq(
        "proximity_threshold" -> JsNumber(o.proximity_threshold),
        "max_wheel_speed" -> JsNumber(o.max_wheel_speed),
        "stay_on_half" -> JsBoolean(o.stay_on_half)))
    }

    implicit def f3: OFormat[Config.BooleanNetwork.Options] = Json.format[Config.BooleanNetwork.Options]

    implicit def f4: OFormat[Config.BooleanNetwork] = Json.format[Config.BooleanNetwork]

    implicit def f5: OFormat[model.BooleanNetwork.Schema] = Json.format[model.BooleanNetwork.Schema]

    implicit def f6: OFormat[Config] = Json.format[Config]
  }

  def fromJson(json: String): Config = {
    import JsonFormats._
    Json.fromJson[Config](Json.parse(json)).get
  }
}


case class Config(simulation: Config.Simulation, robot: Config.Robot, bn: Config.BooleanNetwork) {
  def toJson: String = {
    import Config.JsonFormats._
    Json.toJson(this).toString()
  }

  def combine(variations: Seq[Seq[Config => Config]]): Seq[Config] = {
    /** Generates configurations starting with a seq of basic configuration and a sequence of configuration variations. **/
    @scala.annotation.tailrec
    def combineConfigVariations(configs: Seq[Config], variations: Seq[Seq[Config => Config]]): Seq[Config] = {
      variations match {
        case Nil => configs
        case variation :: tail =>
          val newConfigs = configs.flatMap(config => variation.map(_.apply(config)))
          combineConfigVariations(newConfigs, tail)
      }
    }

    combineConfigVariations(Seq(this), variations)
  }
}