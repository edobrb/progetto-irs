package model

import model.Types.{Fitness, Location}

/**
 * Contains all the data generates from a specific robot in a epoch.
 * A fixed boolean network schema, the states of the boolean network, the location and the fitness values
 * (one for each simulation step).
 *
 * @param bn     the boolean network schema of this test run
 * @param states the sequence of the boolean network states, proximity values, fitness value
 */
case class Epoch(bn: BooleanNetwork.Schema, states: Seq[(BooleanNetwork.State, Fitness, Location)]) {
  def add(s: BooleanNetwork.State, f: Fitness, p: Location): Epoch =
    copy(states = states :+ (s, f, p))

  def fitnessValues: Seq[Fitness] = states.map(_._2)

  def bnStates: Seq[BooleanNetwork.State] = states.map(_._1)

  def locations: Seq[Location] = states.map(_._3)
}

object Epoch {
  def apply(bn: BooleanNetwork.Schema, s: BooleanNetwork.State, f: Fitness, p: Location): Epoch = Epoch(bn, Nil).add(s, f, p)
}
