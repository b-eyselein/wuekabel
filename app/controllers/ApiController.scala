package controllers

import javax.inject.{Inject, Singleton}
import model.jsonFormats.CourseJsonProtocol
import model.persistence.TableDefs
import play.api.Logger
import play.api.libs.json.{JsArray, JsError, JsSuccess}
import play.api.mvc.{AbstractController, ControllerComponents, EssentialAction}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ApiController @Inject()(cc: ControllerComponents, protected val tableDefs: TableDefs)(implicit protected val ec: ExecutionContext)
  extends AbstractController(cc) with NewControllerHelpers {

  private val logger = Logger(classOf[ApiController])

  override protected val adminRightsRequired: Boolean = false

  // Routes

  def courses: EssentialAction = futureWithUser { _ =>
    implicit request =>
      tableDefs.futureAllCourses.map {
        allCourses =>
          val jsonCoursesArray = JsArray(allCourses.map(CourseJsonProtocol.courseJsonFormat.writes))
          Ok(jsonCoursesArray)
      }
  }

  def course(courseId: Int): EssentialAction = withUserAndCourse(courseId) { (_, course) =>
    implicit request => Ok(CourseJsonProtocol.courseJsonFormat.writes(course))
  }

  def newCourse: EssentialAction = futureWithUser { _ =>
    implicit request =>
      request.body.asJson match {
        case None          => Future.successful(???)
        case Some(jsValue) =>

          CourseJsonProtocol.courseJsonFormat.reads(jsValue) match {
            case JsSuccess(newCourse, _) => tableDefs.futureInsertCourse(newCourse) map { _ => Created }
            case JsError(errors)         =>
              errors.foreach(jsE => logger.error(jsE.toString))
              Future.successful(???)
          }

      }
  }

}
