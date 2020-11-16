package utils

import java.util.concurrent.{ArrayBlockingQueue, BlockingQueue}

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

    def copy(bufferSize: Int = 16): (Iterator[T], Iterator[T]) = {
      val values: BlockingQueue[Option[T]] = new ArrayBlockingQueue[Option[T]](bufferSize)

      val duplicate: Iterator[T] = new Iterator[T] {
        var value: Option[T] = None

        override def hasNext: Boolean = {
          value = values.take()
          value.isDefined
        }

        override def next(): T = {
          if (value.isEmpty) {
            values.take().get
          } else {
            val ret = value.get
            value = None
            ret
          }
        }
      }

      val origin: Iterator[T] = new Iterator[T] {
        override def hasNext: Boolean = {
          val v = it.hasNext
          if (!v) values.put(None)
          v
        }

        override def next(): T = {
          val v = it.next
          values.put(Some(v))
          v
        }
      }

      (origin, duplicate)
    }
  }

}
