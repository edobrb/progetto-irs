import java.util.concurrent.ForkJoinPool

import analysis.Functions
import model.TestRun
import model.config.Config

import scala.collection.parallel.CollectionConverters._
import scala.collection.parallel.ForkJoinTaskSupport
import scala.util.{Failure, Success, Try}

object Loader extends App {

  val DATA_FOLDER = "/home/edo/Desktop/progetto-irs/tmp"

  val filenames = Seq("0.1", "0.5", "0.79").flatMap(f => (1 to 30).map(DATA_FOLDER + "/" + f + "-" + _)).par
  filenames.tasksupport = new ForkJoinTaskSupport(new ForkJoinPool(1))
  val res = filenames.flatMap(filename => {
    println(s"Loading $filename ...")
    val data: Try[(Config, Map[String, Seq[TestRun]])] = utils.File.readGzippedLines(filename).map {
      case (value, source) =>
        val config = Config.fromJson(value.head)
        val results = Functions.extractTests(value.map(Functions.toStepInfo).collect { case Some(info) => info })
        source.close()
        (config, results)
    }

    data match {
      case Failure(exception) => println(s"$filename throw an exception: ${exception.getMessage}"); Seq()
      case Success((config, result)) =>
        val (robotId, tests) = result.maxBy {
          case (robotId, tests) => tests.map(_.fitnessValues.last).max
        }
        val bestTest: TestRun = tests.maxBy(_.fitnessValues.last)
        println(bestTest.states.last._2 + " " + bestTest.bn)
        Seq((filename, robotId, bestTest, config))
    }
  })

  val top = res.maxBy(_._3.states.last._2)
  println(top)
  //Test.output(Test.config.copy(bn = Test.bn.copy(initial = Some(top._3.bn))), visualization = true).foreach(println)
}
