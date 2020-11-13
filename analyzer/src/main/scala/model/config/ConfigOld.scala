package model.Config

import play.api.libs.json.{Json, OFormat, _}

object ConfigOld {

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


    implicit def f1: OFormat[ConfigOld.Simulation] = Json.format[ConfigOld.Simulation]

    implicit def f2: OFormat[ConfigOld.Robot] = new OFormat[ConfigOld.Robot] {
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

    implicit def f3: OFormat[ConfigOld.BooleanNetwork.Options] = Json.format[ConfigOld.BooleanNetwork.Options]

    implicit def f4: OFormat[ConfigOld.BooleanNetwork] = Json.format[ConfigOld.BooleanNetwork]

    implicit def f5: OFormat[model.BooleanNetwork.Schema] = Json.format[model.BooleanNetwork.Schema]

    implicit def f6: OFormat[ConfigOld] = Json.format[ConfigOld]
  }

  def fromJson(json: String): ConfigOld = {
    import JsonFormats._
    Json.fromJson[ConfigOld](Json.parse(json)).get
  }
}


case class ConfigOld(simulation: ConfigOld.Simulation, robot: ConfigOld.Robot, bn: ConfigOld.BooleanNetwork) {
  def toJson: String = {
    import ConfigOld.JsonFormats._
    Json.toJson(this).toString()
  }

  def expectedLines: Int = {
    val argosInfoPrints = 0
    val initialConfigOldPrints = 1
    val stepPrints = simulation.experiment_length * simulation.ticks_per_seconds * simulation.robot_count
    val initialBnConfigOldPrints = stepPrints / simulation.network_test_steps
    stepPrints + argosInfoPrints + initialConfigOldPrints + initialBnConfigOldPrints
  }

  def combine(variations: Seq[Seq[ConfigOld => ConfigOld]]): Seq[ConfigOld] =
    utils.Combiner(this, variations)

  /** Map ConfigOlduration to the respective filename */
  def filename: String = {

    case class P[T](f: ConfigOld => T, default: Option[T], name: String, ts: T => String) {
      def isDefined(ConfigOld: ConfigOld): Boolean = default match {
        case Some(value) => f(ConfigOld) != value
        case None => true
      }

      def name(ConfigOld: ConfigOld): String = s"$name=${f.andThen(ts).apply(ConfigOld)}"
    }
    object P {
      def apply[T](name: String, f: ConfigOld => T): P[T] = P(f, None, name, (v: T) => v.toString)

      def apply[T](name: String, default: T, f: ConfigOld => T): P[T] = P(f, Some(default), name, (v: T) => v.toString)

      def apply[T](name: String, default: T, f: ConfigOld => T, ts: T => String): P[T] = P(f, Some(default), name, ts)
    }

    val parametersNames = Seq[P[_]](
      P("el", _.simulation.experiment_length),
      P("rc", _.simulation.robot_count),
      P("bs", _.bn.options.bias),
      P("mir", _.bn.max_input_rewires),
      P("mor", _.bn.max_output_rewires),
      P("sl", _.bn.options.self_loops),
      P("nic", _.bn.options.network_inputs_count),
      P("hv", _.robot.stay_on_half),
      P("fp", _.robot.feed_position),

      P("ts", 10, _.simulation.ticks_per_seconds),
      P("nts", 400, _.simulation.network_test_steps),
      P("pa", true, _.simulation.print_analytics),
      P("pxt", 0.1, _.robot.proximity_threshold),
      P("mws", 5, _.robot.max_wheel_speed),
      P("n", 100, _.bn.options.node_count),
      P("k", 3, _.bn.options.nodes_input_count),
      P("noc", 2, _.bn.options.network_outputs_count),
      P("oonb", true, _.bn.options.override_output_nodes_bias),
      P("mirp", 1.0, _.bn.input_rewires_probability),
      P("morp", 1.0, _.bn.output_rewires_probability),
      P("de", false, _.bn.use_dual_encoding),
      P("ibn", None, (ConfigOld: ConfigOld) => ConfigOld.bn.initial, (_: Option[model.BooleanNetwork.Schema]) => "yes"),
    )

    val name = parametersNames.foldLeft("") {
      case ("", p) if p.isDefined(this) => p.name(this)
      case (name, p) if p.isDefined(this) => name + "-" + p.name(this)
      case (name, _) => name
    }

    utils.Hash.sha256(name)
  }
}