import Main.handleInfo
import model.{BooleanNetwork, BooleanNetworkLife, StepInfo}
import play.api.libs.json._
import utils.File

import scala.util.{Failure, Success, Try}

object MainFromFile extends App {

  implicit val bnFormat: OFormat[BooleanNetwork] = Json.format[BooleanNetwork]
  implicit val siFormat: OFormat[StepInfo] = Json.format[StepInfo]

  val toStepInfo: String => Option[StepInfo] = out => {
    Try(Json.parse(out)) match {
      case Failure(_) => None
      case Success(json) => Json.fromJson[StepInfo](json) match {
        case JsSuccess(info, _) => Some(info)
        case JsError(_) => None
      }
    }
  }

  File.readLines("/home/edo/Desktop/progetto-irs/tmp/lol.txt") match {
    case Failure(exception) =>
    case Success((lines, source)) =>
      val data = lines.map(toStepInfo).collect {
        case Some(value) => value
      }
      val result = data.groupBy(_.id).map {
        case (id, steps) =>
          (id, steps.sortBy(_.step).foldLeft(Seq[BooleanNetworkLife]()) {
          case (Nil, StepInfo(step, id, Some(bn), states, fitness)) =>
            Seq(BooleanNetworkLife(bn, Seq(states)))
          case (l :+ last, StepInfo(step, id, Some(bn), states, fitness)) if bn == last.bn =>
            l :+ (last += states)
          case (l :+ last, StepInfo(step, id, None, states, fitness)) =>
            l :+ (last += states)
          case (l :+ last, StepInfo(step, id, Some(bn), states, fitness)) if bn != last.bn =>
            l :+ last :+ BooleanNetworkLife(bn, Seq(states))
        })
      }
      //val asd = data.groupBy(_.step).map(v => (v._1,v._2.toList)).toList.sortBy(_._1).takeRight(5)


      result.foreach(println)
      source.close()
  }

}
