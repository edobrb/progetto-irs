package utils

import java.util.concurrent.{Executors, Future}

object Parallel {

  implicit class Parallel[T](iterable: Iterable[T]) {
    def parForeach(threads: Int, f: T => ()): Unit = {
      val executor = Executors.newFixedThreadPool(threads)
      val futures: Iterable[Future[_]] = iterable.map(v => executor.submit(new Runnable {
        override def run(): Unit = f(v)
      }))
      futures.foreach(_.get())
      executor.shutdown()
    }
  }

}
