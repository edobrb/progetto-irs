package model.config

import utils.ConfigLens._
import monocle.Lens

case class Variation[K, T](variations: Seq[T],
                           lens: Lens[K, T],
                           name: String,
                           description: T => String = (v: T) => v.toString,
                           collapse: Boolean = false) {
  def apply: Seq[K => K] = variations.lensMap(lens)

  def desc(k: K): String = description(lens.get(k))
}
