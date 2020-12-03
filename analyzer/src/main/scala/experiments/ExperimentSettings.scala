package experiments

import model.config.{Configuration, Variation}

trait ExperimentSettings {

  def defaultConfig: Configuration

  def configVariation: Seq[Variation[Configuration, _]]

  def filter: Configuration => Boolean = _ => true
}

object ExperimentSettings {
  def apply(name: String): ExperimentSettings = {
    //name.split(',').flat
    Map[String, ExperimentSettings](
      "1" -> First,
      "2" -> Second,
      "3" -> Third,
      "4" -> Fourth,
      "5" -> Fifth,
      "6" -> Sixth,
      "7" -> Seventh,
      "8" -> Octave,
      "9" -> Ninth,
      "10" -> Tenth,
    )(name)
  }
}
