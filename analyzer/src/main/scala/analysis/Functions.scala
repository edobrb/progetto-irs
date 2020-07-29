package analysis

import model.{BooleanNetwork, StepInfo, TestRun}
import play.api.libs.json.{JsError, JsSuccess, Json, OFormat}

import scala.util.{Failure, Success, Try}

object Functions {
  implicit val bnFormat: OFormat[BooleanNetwork.Schema] = Json.format[BooleanNetwork.Schema]
  implicit val siFormat: OFormat[StepInfo] = Json.format[StepInfo]

  def toStepInfo(jsonStep: String): Option[StepInfo] =
    Try(Json.parse(jsonStep)) match {
      case Success(json) => Json.fromJson[StepInfo](json) match {
        case JsSuccess(info, _) => Some(info)
        case JsError(_) => None
      }
      case Failure(_) => None
    }

  def extractTests(data: Iterable[StepInfo]): Map[String, Seq[TestRun]] = data.groupBy(_.id).map {
    case (id, steps) =>
      (id, steps.toSeq.sortBy(_.step).foldLeft(Seq[TestRun]()) {
        case (l :+ last, StepInfo(step, id, None, states, fitness, proximity)) =>
          l :+ (last += (states, proximity, fitness))
        case (l :+ last, StepInfo(step, id, Some(bn), states, fitness, proximity)) if bn == last.bn =>
          l :+ (last += (states, proximity, fitness))
        case (l :+ last, StepInfo(step, id, Some(bn), states, fitness, proximity)) if bn != last.bn =>
          l :+ last :+ TestRun(bn, Seq((states, proximity, fitness)))
        case (Nil, StepInfo(step, id, Some(bn), states, fitness, proximity)) =>
          Seq(TestRun(bn, Seq((states, proximity, fitness))))
      })
  }
}
