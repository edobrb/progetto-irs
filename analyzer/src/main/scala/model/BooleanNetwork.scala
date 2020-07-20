package model

object BooleanNetwork {
  type State = Seq[Boolean]
}
case class BooleanNetwork(functions: Seq[Seq[Boolean]],
                          connections: Seq[Seq[Int]],
                          inputs: Seq[Int],
                          outputs: Seq[Int],
                          overridden_output_functions: Option[Seq[Seq[Boolean]]])


