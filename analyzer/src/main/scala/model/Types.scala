package model

object Types {
  type Fitness = Double
  type ProximityValues = Seq[Double]
  type RobotId = String

  //(X,Y,Z angle)
  type Location = (Double, Double, Double)
}
