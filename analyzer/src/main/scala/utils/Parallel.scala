package utils

import java.util.concurrent.{Callable, Executors, Future}

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

}
