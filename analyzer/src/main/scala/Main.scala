import java.io.File

import model.{BooleanNetwork, StepInfo}
import play.api.libs.json._

import scala.sys.process.{Process, ProcessLogger}
import scala.util.{Failure, Success, Try}



object Main extends App {
  implicit val bnFormat: OFormat[BooleanNetwork] = Json.format[BooleanNetwork]
  implicit val siFormat: OFormat[StepInfo] = Json.format[StepInfo]

  val outPipe: String => Unit = out => {
    Try(Json.parse(out)) match {
      case Failure(_) => println("Failed to parse json: " + out)
      case Success(json) => Json.fromJson[StepInfo](json) match {
        case JsSuccess(info, _) => handleInfo(info)
        case JsError(_) => println("Failed to convert json: " + json)
      }
    }
  }

  val data = scala.collection.mutable.Map[(String, Int), StepInfo]()
  def handleInfo(info:StepInfo): Unit = {
    data += (info.id, info.step) -> info
    if(info.id == "fb1") {
      println(info.fitness+" "+info.boolean_network.isDefined)
    }
  }


  Process(s"argos3 -c simulation.argos", new File("/home/edo/Desktop/progetto-irs")) run ProcessLogger(outPipe, print)


}