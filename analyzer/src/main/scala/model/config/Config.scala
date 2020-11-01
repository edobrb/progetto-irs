package model.config

import play.api.libs.json.{Json, OFormat, _}

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
                   stay_on_half: Boolean,
                   feed_position: Boolean)


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
          (obj.get("proximity_threshold"), obj.get("max_wheel_speed"), obj.get("stay_on_half"), obj.get("feed_position")) match {
            case (Some(JsNumber(pt)), Some(JsNumber(mws)), None, None) => JsSuccess(Robot(pt.toDouble, mws.toDouble, stay_on_half = false, feed_position = false))
            case (Some(JsNumber(pt)), Some(JsNumber(mws)), Some(JsBoolean(soh)), None) => JsSuccess(Robot(pt.toDouble, mws.toDouble, soh, feed_position = false))
            case (Some(JsNumber(pt)), Some(JsNumber(mws)), None, Some(JsBoolean(fp))) => JsSuccess(Robot(pt.toDouble, mws.toDouble, stay_on_half = false, fp))
            case (Some(JsNumber(pt)), Some(JsNumber(mws)), Some(JsBoolean(soh)), Some(JsBoolean(fp))) => JsSuccess(Robot(pt.toDouble, mws.toDouble, soh, fp))
            case _ => JsError()
          }
        case _ => JsError()
      }

      override def writes(o: Robot): JsObject = JsObject(Seq(
        "proximity_threshold" -> JsNumber(o.proximity_threshold),
        "max_wheel_speed" -> JsNumber(o.max_wheel_speed),
        "stay_on_half" -> JsBoolean(o.stay_on_half),
        "feed_position" -> JsBoolean(o.feed_position)))
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

  def expectedLines: Int = {
    val argosInfoPrints = 19
    val initialConfigPrints = 1
    simulation.experiment_length * simulation.ticks_per_seconds * simulation.robot_count + simulation.robot_count * 2 + argosInfoPrints + initialConfigPrints
  }

  def combine(variations: Seq[Seq[Config => Config]]): Seq[Config] = {
    /** Generates configurations starting with a seq of basic configuration and a sequence of configuration variations. * */
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

  /** Map configuration to the respective filename */
  def filename: String =
    s"el=${simulation.experiment_length}-rc=${simulation.robot_count}-bs=${bn.options.bias}-" +
      s"mir=${bn.max_input_rewires}-mor=${bn.max_output_rewires}-sl=${bn.options.self_loops}-" +
      s"nic=${bn.options.network_inputs_count}-hv=${robot.stay_on_half}-fp=${robot.stay_on_half && robot.feed_position}"
}
