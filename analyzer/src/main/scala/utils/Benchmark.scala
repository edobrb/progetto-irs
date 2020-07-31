package utils

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.duration._
object Benchmark {

  def time[T](f: => T):(T,FiniteDuration) = {
    val start = System.currentTimeMillis()
    val res = f
    (res, (System.currentTimeMillis() - start) millis)
  }
}
