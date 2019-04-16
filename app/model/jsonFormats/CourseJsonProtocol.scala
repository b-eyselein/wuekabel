package model.jsonFormats

import model.Course
import play.api.libs.json._
import play.api.libs.functional.syntax._
import model.Consts._

object CourseJsonProtocol {

  private val courseJsonWrites: Writes[Course] = (
    (__ \ idName).write[Int] and
      (__ \ shortNameName).write[String] and
      (__ \ titleName).write[String]
    ) (unlift(Course.unapply))

  private val courseJsonReads: Reads[Course] = (
    (__ \ idName).read[Int] and
      (__ \ shortNameName).read[String] and
      (__ \ titleName).read[String]
    ) (Course.apply _)

  val courseJsonFormat: Format[Course] = Format[Course](courseJsonReads, courseJsonWrites)

}
