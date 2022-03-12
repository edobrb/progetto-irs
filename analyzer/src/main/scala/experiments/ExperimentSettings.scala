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
      "1" -> E1,
      "2" -> E2,
      "3" -> E3,
      "4" -> E4,
      "5" -> E5,
      "6" -> E6,
      "7" -> E7,
      "8" -> E8,
      "9" -> E9,
      "9dual" -> E9Dual,
      "9p" -> E9Perturbations,
      "9bis" -> E9Bis,
      "9one" -> E9One,
      "11" -> E11,
      "12" -> E12,
      "13" -> E13,
      "14" -> E14,
      "15" -> E15,
    )(name)
  }
}
