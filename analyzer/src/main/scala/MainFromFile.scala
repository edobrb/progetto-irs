import analysis.Functions._
import model.TestRun
import utils.File

import scala.util.{Failure, Success}

object MainFromFile extends App {
  File.readGzippedLines("/home/edo/Desktop/progetto-irs/tmp/data2") match {
    case Success((lines, source)) =>
      val data = lines.map(toStepInfo).collect {
        case Some(value) => value
      }
      val result: Map[String, Seq[TestRun]] = extractTests(data)
      println(result.size)
      val max = result.maxBy(_._2.map(_.fitnessValues.last).max)
      val maxT = max._2.maxBy(_.fitnessValues.last)
      println(maxT)
      source.close()

    case Failure(exception) =>
  }

}
