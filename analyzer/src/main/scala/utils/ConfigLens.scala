package utils

import model.config.Configuration
import monocle.Lens
import monocle.macros.GenLens

object ConfigLens {
  def lens: GenLens[Configuration] = GenLens[Configuration]

  implicit class SeqLens[T](seq: Seq[T]) {
    def lensMap[K](lens: Lens[K, T]): Seq[K => K] = seq.map(lens.set)
  }

  implicit class RichLens[S, T](lens: Lens[S, T]) {
    def and[K](other: Lens[S, K]): Lens[S, (T, K)] =
      Lens[S, (T, K)](s => (lens.get(s), other.get(s)))(b => lens.set(b._1).andThen(other.set(b._2)))
  }

}
