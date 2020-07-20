package model

case class BooleanNetworkLife(bn: BooleanNetwork, states: Seq[BooleanNetwork.State]) {
  def +=(s: BooleanNetwork.State): BooleanNetworkLife = this.copy(states = states :+ s)
}
