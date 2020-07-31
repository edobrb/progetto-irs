package model

import model.Types.{Fitness, ProximityValues}

/**
 * Contains all the data generates from a specific robot in a whole test run.
 * A fixed boolean network schema, the states of the boolean network, the proximity sensors and the fitness values
 * (one for each simulation step).
 *
 * @param bn     the boolean network schema of this test run
 * @param states the sequence of the boolean network states, proximity values, fitness value
 */
case class TestRun(bn: BooleanNetwork.Schema, states: Seq[(BooleanNetwork.State, Fitness)]) {
  def add(s: BooleanNetwork.State, f: Fitness): TestRun =
    this.copy(states = states :+ (s, f))

  def fitnessValues: Seq[Fitness] = states.map(_._2)

  def bnStates: Seq[BooleanNetwork.State] = states.map(_._1)
}
