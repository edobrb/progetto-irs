package experiments

import model.config.{Configuration, Variation}

trait ExperimentSettings {

  def defaultConfig: Configuration

  def configVariation: Seq[Variation[Configuration, _]]
}

object ExperimentSettings {
  def apply(name: String): ExperimentSettings = Map[String, ExperimentSettings](
    "first" -> First,
    "second" -> Second
  )(name)
}
