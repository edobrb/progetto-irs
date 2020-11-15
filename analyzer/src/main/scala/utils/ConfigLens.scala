package utils

import model.config.Configuration
import monocle.Lens
import monocle.macros.GenLens

object ConfigLens {
  def lens: GenLens[Configuration] = GenLens[Configuration]

  implicit class SeqLens[T](seq: Seq[T]) {
    def lensMap[K](lens: Lens[K, T]): Seq[K => K] = seq.map(lens.set)
  }

}
