package utils

object MyMath {
  def sigmoid(x: Double): Double = 1.0 / (1 + Math.exp(-x))

  def sigmoidD(x: Double): Double = sigmoid(x) * (1 - sigmoid(x))

  def h(entropy: Double, targetEntropy: Double, alpha: Double, beta: Double): Double =
    sigmoidD((entropy - targetEntropy) * (if (entropy < targetEntropy) alpha else beta)) * 4;
}
