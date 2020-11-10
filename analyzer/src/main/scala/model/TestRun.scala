package model

import model.Types.{Fitness, Location}

/**
 * Contains all the data generates from a specific robot in a whole test run.
 * A fixed boolean network schema, the states of the boolean network, the proximity sensors and the fitness values
 * (one for each simulation step).
 *
 * @param bn     the boolean network schema of this test run
 * @param states the sequence of the boolean network states, proximity values, fitness value
 */
case class TestRun(bn: BooleanNetwork.Schema, states: Seq[(BooleanNetwork.State, Fitness, Location)]) {
  def add(s: BooleanNetwork.State, f: Fitness, p: Location): TestRun =
    copy(states = states :+ (s, f, p))

  def fitnessValues: Seq[Fitness] = states.map(_._2)

  def bnStates: Seq[BooleanNetwork.State] = states.map(_._1)

  def positions: Seq[Location] = states.map(_._3)
}

object TestRun {
  def apply(bn: BooleanNetwork.Schema, s: BooleanNetwork.State, f: Fitness, p: Location): TestRun = TestRun(bn, Nil).add(s, f, p)
}
