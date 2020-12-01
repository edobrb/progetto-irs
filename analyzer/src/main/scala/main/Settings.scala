package main

import experiments.ExperimentSettings
import model.config.Configuration

object Settings {

  def selectedExperiment(implicit args: Array[String]): ExperimentSettings =
    ExperimentSettings(Args.CONFIGURATION)

  /** All configuration combinations */
  def configurations(implicit args: Array[String]): Seq[Configuration] =
    utils.Combiner(selectedExperiment.defaultConfig, selectedExperiment.configVariation.map(_.apply))
      .distinct.filter(selectedExperiment.filter)

  /** Filenames of experiments and the relative config */
  def experiments(implicit args: Array[String]): Seq[(String, Configuration, Int)] = {
    /** Configuration repetitions for statistical accuracy. */
    configurations.flatMap {
      config =>
        def setSeed(i: Int): Configuration = {
          val name = config.filename + "-" + i
          config
            .setSimulationSeed(Some(Math.abs((name + "-simulation").hashCode)))
            .setControllersSeed(Some(Math.abs((name + "-controller").hashCode)))
        }

        Args.REPETITIONS.map(i => (config.filename + "-" + i, setSeed(i), i))
    }
  }
}
