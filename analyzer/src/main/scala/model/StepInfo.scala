package model

case class StepInfo(step: Int, id: String, boolean_network: Option[BooleanNetwork], states: BooleanNetwork.State, fitness: Double, proximity: Seq[Double])
