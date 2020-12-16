package utils

object Entropy {
  def shannon(probabilities: Iterable[Double], base: Double = 2): Double =
    -probabilities.foldLeft(0.0)({
      case (sum, p) => sum + (p * (Math.log(p) / Math.log(base)))
    })
}
