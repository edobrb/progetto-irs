package utils

import java.util.concurrent.{ArrayBlockingQueue, BlockingQueue, Callable, Executors, Future}
import scala.annotation.tailrec

object Parallel {

  def run2[A, B](a: => A, b: => B): (A, B) = {
    val executor = Executors.newFixedThreadPool(2)
    val futureA = executor.submit(() => a)
    val futureB = executor.submit(() => b)
    (futureA.get, futureB.get)
  }

  implicit class Parallel[T](iterable: Iterable[T]) {
    def parForeach(threads: Int, f: T => ()): Unit = {
      val executor = Executors.newFixedThreadPool(threads)
      val futures: Iterable[Future[_]] = iterable.map(v => executor.submit(new Runnable {
        override def run(): Unit = f(v)
      }))
      futures.foreach(_.get())
      executor.shutdown()
    }

    def parMap[R](threads: Int, f: T => R): Iterable[R] = {
      val executor = Executors.newFixedThreadPool(threads)
      val futures: Iterable[Future[R]] = iterable.map(v => executor.submit(() => f(v)))
      val result = futures.map(_.get())
      executor.shutdown()
      result
    }

    def parFlatmap[R](threads: Int, f: T => Iterable[R]): Iterable[R] = {
      val executor = Executors.newFixedThreadPool(threads)
      val futures: Iterable[Future[Iterable[R]]] = iterable.map(v => executor.submit(() => f(v)))
      val result = futures.map(_.get())
      executor.shutdown()
      result.flatten
    }
  }

  implicit class ParallelIterator[T](iterable: Iterator[T]) {

    def parMap[R](threads: Int, f: T => R, receiveBufferSize: Int = 32, mappedBufferSize: Int = 32): Iterator[R] = {
      val values: BlockingQueue[Option[T]] = new ArrayBlockingQueue[Option[T]](receiveBufferSize)
      val mappedValues: BlockingQueue[Option[R]] = new ArrayBlockingQueue[Option[R]](mappedBufferSize)

      val executor = Executors.newFixedThreadPool(threads + 1)

      executor.submit(new Runnable {
        override def run(): Unit = {
          iterable.map(Some.apply).foreach(values.put)
          (0 until threads).foreach(_ => values.put(None))
        }
      })

      (0 until threads).map(_ => executor.submit(new Runnable {
        override def run(): Unit = {
          Iterator.continually(values.take()).takeWhile(_.nonEmpty).collect {
            case Some(value) => f(value)
          }.map(Some.apply).foreach(mappedValues.put)
          mappedValues.put(None)
        }
      }))

      new Iterator[R] {
        var noneReceived = 0
        var nextE: Option[R] = None

        @tailrec
        override def hasNext: Boolean = if (noneReceived == threads) {
          executor.shutdown()
          false
        } else {
          nextE = mappedValues.take()
          nextE match {
            case Some(_) => true
            case None =>
              noneReceived = noneReceived + 1
              hasNext
          }
        }

        override def next(): R = nextE.get
      }
    }

  }


}
