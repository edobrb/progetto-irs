package utils

import java.io.File

import model.config.Config

import scala.sys.process.Process

object Argos {
  def runSimulation(workingDir: String, simulationFile: String): LazyList[String] =
    Process(s"argos3 -c $simulationFile", new File(workingDir)).lazyLines

  def runConfiguredSimulation(workingDir: String, simulationFile: String, config: Config): LazyList[String] = {
    val escapedConfig = "\\\'" + config.toJson.replace("\"", "\\\"") + "\\\'"
    Process(Seq("./pargos", simulationFile,
      s"--CONFIG=$escapedConfig",
      s"--TICKS=${config.simulation.ticks_per_seconds}",
      s"--LENGTH=${config.simulation.experiment_length}"),
      new File(workingDir)).lazyLines
  }
}
