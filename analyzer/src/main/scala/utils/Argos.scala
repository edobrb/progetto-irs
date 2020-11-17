package utils

import java.io.{BufferedReader, File, InputStream, InputStreamReader}

import model.config.Configuration
import utils.ConfigLens.lens

import scala.jdk.CollectionConverters._
import scala.sys.process.{Process, ProcessIO}
import scala.util.Try

object Argos {
  def runSimulation(workingDir: String, simulationFile: String): LazyList[String] =
    Process(s"argos3 -c $simulationFile", new File(workingDir)).lazyLines

  def runConfiguredSimulation(workingDir: String, config: Configuration, visualization: Boolean): Iterator[String] = {
    val escapedConfig = "\\\'" + lens(_.simulation.argos).set("")(config).toJson.replace("\"", "\\\"") + "\\\'"

    var output: InputStream = null
    val lock = new Object()
    val io = new ProcessIO(
      stdin => (),
      stdout => lock.synchronized {
        output = stdout
        lock.notify()
      }, stderr => {
        val decoder = new InputStreamReader(stderr)
        val buffered = new BufferedReader(decoder)
        buffered.lines().iterator().asScala.foreach(s => println(s"ERR: $s"))
      })
    Process(Seq("./pargos", config.simulation.argos,
      s"--CONFIG=$escapedConfig",
      s"--TICKS=${config.simulation.ticks_per_seconds}",
      s"--LENGTH=${config.simulation.experiment_length}",
      s"--ROBOT_COUNT=${config.simulation.robot_count}",
      s"--RANDOM_SEED=${config.simulation.simulation_random_seed.map("random_seed=" + _).getOrElse("")}",
      s"--VISUAL=${if (visualization) "visualization" else "none"}"),
      new File(workingDir)).run(io)

    lock.synchronized {
      while (output == null) Try(lock.wait())
    }

    val decoder = new InputStreamReader(output)
    val buffered = new BufferedReader(decoder)
    buffered.lines().iterator().asScala
  }
}
