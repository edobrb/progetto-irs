package model

import model.Types.{Fitness, Location}

/**
 * Contains all the data generates from a specific robot in a epoch.
 * A fixed boolean network schema, the states of the boolean network, the location and the fitness values
 * (one for each simulation step).
 *
 * @param bn     the boolean network schema of this test run
 * @param states the sequence of the boolean network inputs, proximity values, fitness value
 */
case class Epoch(bn: BooleanNetwork, states: Seq[(Seq[Boolean], Fitness, Location)]) {
  def add(i: Seq[Boolean], f: Fitness, p: Location): Epoch =
    copy(states = states :+ (i, f, p))

  def fitnessValues: Seq[Fitness] = states.map(_._2)

  def bnInputs: Seq[Seq[Boolean]] = states.map(_._1)

  def locations: Seq[Location] = states.map(_._3)
}

object Epoch {
  def apply(bn: BooleanNetwork, i: Seq[Boolean], f: Fitness, p: Location): Epoch = Epoch(bn, Nil).add(i, f, p)
}
