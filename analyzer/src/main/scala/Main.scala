import java.io.File

import analysis.Functions._

import scala.sys.process.Process

object Main extends App {

  def data: LazyList[String] = Process(s"argos3 -c simulation.argos", new File("/home/edo/Desktop/progetto-irs")).lazyLines

  val result = extractTests(data.map(toStepInfo).collect { case Some(info) => info })

  println(result.size)
}