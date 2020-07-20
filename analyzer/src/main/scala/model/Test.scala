package model

case class Test(bn: BooleanNetwork, states: Seq[BooleanNetwork.State], proximities: Seq[Seq[Double]], fitnesses:Seq[Double]) {
  def +=(s: BooleanNetwork.State, p: Seq[Double], f:Double): Test =
    this.copy(states = states :+ s, proximities = proximities :+ p, fitnesses = fitnesses :+ f)
}
