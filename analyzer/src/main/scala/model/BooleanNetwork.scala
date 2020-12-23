

package model

import scala.annotation.tailrec
import scala.util.Random

object BooleanNetwork {
  def random(n: Int, k: Int, bias: Double): BooleanNetwork = {
    val states = IndexedSeq.fill(n)(false)
    val connections = IndexedSeq.fill(n)(IndexedSeq.fill(k)(Random.between(0, n)))
    val functions = IndexedSeq.fill(n)(IndexedSeq.fill(1 << k)(Random.nextDouble() < bias))
    BooleanNetwork(functions, connections, states, IndexedSeq.empty,  IndexedSeq.empty, None).randomizeStates(0.5)
  }
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
case class BooleanNetwork(functions: IndexedSeq[IndexedSeq[Boolean]],
                          connections: IndexedSeq[IndexedSeq[Int]],
                          states: IndexedSeq[Boolean],
                          inputs: IndexedSeq[Int],
                          outputs: IndexedSeq[Int],
                          overridden_output_functions: Option[Seq[Seq[Boolean]]]) {

  def withInputs(inputsValues: Seq[Boolean]): BooleanNetwork =
    this.copy(states = states.zipWithIndex.map {
      case (_, i) if inputs.contains(i) =>
        val inputIndex = inputs.indexOf(i)
        inputsValues(inputIndex)
      case (state, _) => state
    })

  def steps(n: Int = 1): BooleanNetwork =
    (0 until n).foldLeft(this)({
      case (bn, _) => bn.step()
    })

  def step():BooleanNetwork = this.copy(states = this.states.indices.map { i =>
    val column = this.connections(i).indices.foldLeft(0)({
      case (sum, connectionIndex) if this.states(this.connections(i)(connectionIndex)) => sum + (1 << connectionIndex)
      case (sum, _) => sum
    })
    this.functions(i)(column)
  })

  def statesMap(steps: Int, perturbation: BooleanNetwork => BooleanNetwork = identity): Map[BooleanNetwork, Int] = {
    (0 until steps).foldLeft((Map[BooleanNetwork, Int](this -> 1), this))({
      case ((map, bn), _) =>
        val newBn = perturbation(bn).steps()
        val newMap = if (map.contains(newBn)) map.updated(newBn, map(newBn) + 1)
        else map + (newBn -> 1)
        (newMap, newBn)
    })._1
  }

  def randomizeStates(p: Double): BooleanNetwork =
    this.copy(states = this.states.map(_ => Random.nextDouble() < p))

  def statesHammingDistance(other: BooleanNetwork): Int =
    this.states.indices.count(i => this.states(i) != other.states(i))

  def invertRandomInputs(q: Int): BooleanNetwork =
    this.copy(states = Random.shuffle(this.inputs).take(q).foldLeft(this.states)({
      case (modifiedStates, input) =>
        modifiedStates.updated(input, !modifiedStates(input))
    }))

  def invertRandomStates(q: Int): BooleanNetwork =
    this.copy(states = Random.shuffle(this.states.indices.toList).take(q).foldLeft(this.states)({
      case (modifiedStates, input) =>
        modifiedStates.updated(input, !modifiedStates(input))
    }))

  def invertState(index:Int):BooleanNetwork = copy(states = states.updated(index, !states(index)))

  def prettyStatesString: String = states.map(if (_) "1" else "0").mkString

  def fsm(limit: Option[Int] = None): Map[BooleanNetwork, Seq[BooleanNetwork]] = {
    val inputSet = (0 until (1 << inputs.size)).map(v => Seq.fill(inputs.size)(false).zipWithIndex.map(i => (v & (1 << i._2)) > 0))

    def successors(bn: BooleanNetwork): Seq[BooleanNetwork] = {
      inputSet.map(bn.withInputs).map(_.steps())
    }

    @tailrec
    def compute(states: Map[BooleanNetwork, Seq[BooleanNetwork]]): Map[BooleanNetwork, Seq[BooleanNetwork]] =
      if (limit.exists(_ < states.size)) {
        states
      } else {
        states.collectFirst {
          case (bn, Nil) => bn
        } match {
          case Some(bn) =>
            val s = successors(bn)
            val newStates = s.foldLeft(states)({
              case (foldStates, successor) if foldStates.contains(successor) => foldStates
              case (foldStates, successor) => foldStates.updated(successor, Nil)
            }).updated(bn, s)
            compute(newStates)
          case None => states
        }
      }

    compute(Map(this -> Nil))
  }

}

