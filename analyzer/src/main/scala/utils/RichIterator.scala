package utils

object RichIterator {

  implicit class RichIterator[T](it: Iterator[T]) {
    def +:(value: T): Iterator[T] = new Iterator[T] {

      var head: Option[T] = Some(value)

      override def hasNext: Boolean = it.hasNext || head.isDefined

      override def next(): T = head match {
        case None => it.next()
        case Some(v) => head = None; v
      }
    }
  }

}
