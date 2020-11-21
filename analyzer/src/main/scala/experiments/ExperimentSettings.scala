package experiments

import model.config.{Configuration, Variation}

trait ExperimentSettings {

  def defaultConfig: Configuration

  def configVariation: Seq[Variation[Configuration, _]]
}

object ExperimentSettings {
  def apply(name: String): ExperimentSettings = Map[String, ExperimentSettings](
    "1" -> First,
    "2" -> Second,
    "3" -> Third,
    "4" -> Fourth,
    "5" -> Fifth,
    "6" -> Sixth,
  )(name)
}
