package utils

import java.util.concurrent.ForkJoinPool

import scala.collection.parallel.ForkJoinTaskSupport
import scala.collection.parallel.immutable.ParIterable

object RichParIterable {
  implicit class RichParIterable[T](it:ParIterable[T]) {
    def parallelism(v:Int):ParIterable[T] = {
      it.tasksupport = new ForkJoinTaskSupport(new ForkJoinPool(v))
      it
    }
  }
}
