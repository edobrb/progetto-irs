package model

object BooleanNetwork {
}

/**
 * The definition of a boolean network schema (the state is not included).
 *
 * @param functions                   truth tables of boolean network nodes
 * @param connections                 connections indexes of nodes
 * @param states                      the state of the netowrk
 * @param inputs                      indexes of input nodes
 * @param outputs                     indexes of output nodes
 * @param overridden_output_functions optionally the truth tables of the output nodes
 */
case class BooleanNetwork(functions: Seq[Seq[Boolean]],
                          connections: Seq[Seq[Int]],
                          states: Seq[Boolean],
                          inputs: Seq[Int],
                          outputs: Seq[Int],
                          overridden_output_functions: Option[Seq[Seq[Boolean]]]) extends (Seq[Boolean] => BooleanNetwork) {

  override def apply(inputsValues: Seq[Boolean]): BooleanNetwork = {
    val oldStates = states.zipWithIndex.map {
      case (_, i) if inputs.contains(i) =>
        val inputIndex = inputs.indexOf(i)
        inputsValues(inputIndex)
      case (state, _) => state
    }

    val newStates = states.indices.map { i=>
      val column = connections(i).zipWithIndex.foldLeft(0)({
        case (sum, (connection, index)) if oldStates(connection) => sum + (1 << index)
        case (sum, _) => sum
      })
      functions(i)(column)
    }

    this.copy(states = newStates)
  }
}





