
object Checker extends App {
  val notValid = (Analyzer.rawData.map {
    v => {
      val config = v.config
      val expectedTestsCount = config.simulation.experiment_length * config.simulation.ticks_per_seconds / config.simulation.network_test_steps
      if (v.fitness_values.size != expectedTestsCount) {
        Some(v.filename)
      } else {
        None
      }
    }
  } collect {
    case Some(value) => value
  }).toSet

  println("Not valid files:")
  notValid foreach println
  println(notValid.size)
}
