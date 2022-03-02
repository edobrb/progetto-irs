package model.config

import utils.ConfigLens._
import monocle.Lens

trait Variation[K, V] {
  def name: String

  def legendName: String

  def variations: Seq[V]

  def applyVariation(v: V)(k: K): K

  def getVariation(k: K): V

  def desc(k: K): String

  def apply: Seq[K => K] = variations.map(v => applyVariation(v))

  def collapse: Boolean = false

  def showDivided: Boolean = true
}

object Variation {
  def apply[K, V](variations: Seq[V], lens: Lens[K, V], name: String, description: V => String = (v: V) => v.toString,
                  collapse: Boolean = false, showDivided: Boolean = false): Variation[K, V] =
    LensVariation(variations, lens, name, name, description, collapse, showDivided)

  def apply2[K, V](variations: Seq[V], lens: Lens[K, V], name: String, legendName:String, description: V => String = (v: V) => v.toString,
                  collapse: Boolean = false, showDivided: Boolean = false): Variation[K, V] =
    LensVariation(variations, lens, name, legendName, description, collapse, showDivided)

  def lens2[K, V](variations: Seq[V], lens: Lens[K, V], name: String, legendName: String, description: V => String = (v: V) => v.toString,
                  collapse: Boolean = false, showDivided: Boolean = false): Variation[K, V] =
    LensVariation(variations, lens, name, legendName, description, collapse, showDivided)

  def normal[K, V](variations: Seq[V], setter: (V, K) => K, getter: K => V, name: String, description: V => String,
                   collapse: Boolean = false, showDivided: Boolean = false): Variation[K, V] =
    FunctionalVariation(variations, setter, getter, name, name, k => description(getter(k)), collapse, showDivided)

  def normal2[K, V](variations: Seq[V], setter: (V, K) => K, getter: K => V, name: String, legendName: String, description: V => String,
                   collapse: Boolean = false, showDivided: Boolean = false): Variation[K, V] =
    FunctionalVariation(variations, setter, getter, name, legendName, k => description(getter(k)), collapse, showDivided)

  case class LensVariation[K, V](override val variations: Seq[V],
                                 lens: Lens[K, V],
                                 override val name: String,
                                 override val legendName: String,
                                 description: V => String = (v: V) => v.toString,
                                 override val collapse: Boolean,
                                 override val showDivided: Boolean) extends Variation[K, V] {
    override def applyVariation(v: V)(k: K): K = lens.set(v)(k)

    override def desc(k: K): String = description(lens.get(k))

    override def getVariation(k: K): V = lens.get(k)
  }

  case class FunctionalVariation[K, V](override val variations: Seq[V],
                                       setter: (V, K) => K,
                                       getter: K => V,
                                       override val name: String,
                                       override val legendName: String,
                                       description: K => String,
                                       override val collapse: Boolean,
                                       override val showDivided: Boolean) extends Variation[K, V] {
    override def applyVariation(v: V)(k: K): K = setter(v, k)

    override def desc(k: K): String = description(k)

    override def getVariation(k: K): V = getter(k)
  }

}


