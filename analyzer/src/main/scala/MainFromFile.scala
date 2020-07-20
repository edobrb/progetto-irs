import Main.handleInfo
import model.{BooleanNetwork, Test, StepInfo}
import play.api.libs.json._
import utils.File
import scala.collection.parallel.CollectionConverters._
import scala.util.{Failure, Success, Try}

object MainFromFile extends App {

  implicit val bnFormat: OFormat[BooleanNetwork] = Json.format[BooleanNetwork]
  implicit val siFormat: OFormat[StepInfo] = Json.format[StepInfo]

  def toStepInfo(jsonStep: String): Option[StepInfo] =
    Try(Json.parse(jsonStep)) match {
      case Success(json) => Json.fromJson[StepInfo](json) match {
        case JsSuccess(info, _) => Some(info)
        case JsError(_) => None
      }
      case Failure(_) => None
    }

  def extractTests(data: Iterable[StepInfo]): Map[String, Seq[Test]] = data.groupBy(_.id).map {
    case (id, steps) =>
      (id, steps.toSeq.sortBy(_.step).foldLeft(Seq[Test]()) {
        case (l :+ last, StepInfo(step, id, None, states, fitness, proximity)) =>
          l :+ (last += (states, proximity, fitness))
        case (l :+ last, StepInfo(step, id, Some(bn), states, fitness, proximity)) if bn == last.bn =>
          l :+ (last += (states, proximity, fitness))
        case (l :+ last, StepInfo(step, id, Some(bn), states, fitness, proximity)) if bn != last.bn =>
          l :+ last :+ Test(bn, Seq(states), Seq(proximity), Seq(fitness))
        case (Nil, StepInfo(step, id, Some(bn), states, fitness, proximity)) =>
          Seq(Test(bn, Seq(states), Seq(proximity), Seq(fitness)))
      })
  }

  File.readGzippedLines("/home/edo/Desktop/progetto-irs/tmp/data1") match {
    case Success((lines, source)) =>
      val data = lines.map(toStepInfo).collect {
        case Some(value) => value
      }
      val result = extractTests(data)
      //val lol = data.groupMapReduce(v => (v.id, v.step))(identity)((a,_) => a)

      println(result.size)

      val max = result.maxBy(_._2.map(_.fitnesses.last).max)
      val maxT = max._2.maxBy(_.fitnesses.last)
      println(maxT)
      source.close()

    case Failure(exception) =>
  }

}
