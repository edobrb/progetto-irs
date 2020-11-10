package utils

object Combiner {
  def apply[T](initial: T, variations: Seq[Seq[T => T]]): Seq[T] = {
    /** Generates configurations starting with a seq of basic configuration and a sequence of configuration variations. * */
    @scala.annotation.tailrec
    def combineConfigVariations(values: Seq[T], variations: Seq[Seq[T => T]]): Seq[T] = {
      variations match {
        case Nil => values
        case variation :: tail =>
          val newConfigs = values.flatMap(config => variation.map(_.apply(config)))
          combineConfigVariations(newConfigs, tail)
      }
    }

    combineConfigVariations(Seq(initial), variations)
  }
}
