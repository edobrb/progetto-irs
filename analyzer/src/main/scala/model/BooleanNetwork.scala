package model

object BooleanNetwork {
  /**
   * The state of a boolean network.
   */
  type State = Seq[Boolean]

  /**
   * The definition of a boolean network schema (the state is not included).
   *
   * @param functions truth tables of boolean network nodes
   * @param connections connections indexes of nodes
   * @param inputs indexes of input nodes
   * @param outputs indexes of output nodes
   * @param overridden_output_functions optionally the truth tables of the output nodes
   */
  case class Schema(functions: Seq[Seq[Boolean]],
                    connections: Seq[Seq[Int]],
                    inputs: Seq[Int],
                    outputs: Seq[Int],
                    overridden_output_functions: Option[Seq[Seq[Boolean]]])

}



