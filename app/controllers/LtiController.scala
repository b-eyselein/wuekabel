package controllers

import javax.inject.{Inject, Singleton}
import model.Consts.idName
import model.{Course, FormMappings, LtiFormValues, LtiToolProxyRegistrationRequestFormValues, User}
import model.persistence.TableDefs
import play.api.data.Form
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class LtiController @Inject()(cc: ControllerComponents, protected val tableDefs: TableDefs)(implicit protected val ec: ExecutionContext)
  extends AbstractController(cc) with ControllerHelpers with play.api.i18n.I18nSupport {

  override protected val adminRightsRequired: Boolean = false

  // Routes

  private def selectOrInsertUser(username: String): Future[User] = tableDefs.futureUserByUserName(username) flatMap {
    case Some(user) => Future.successful(user)
    case None       =>
      val newUser = User(username)
      tableDefs.futureInsertUser(newUser) map {
        case true  => newUser
        case false => ???
      }
  }

  private def selectOrInsertCourse(id: String, name: String): Future[Course] =
  //    tableDefs.futureCourseById(id) flatMap {
  //    case Some(course) => Future.successful(course)
  //    case None         =>
  //      val newCourse = Course(id, name)
  //      tableDefs.futureInsertCourse(newCourse) map {
  //        case true  => newCourse
  //        case false => ???
  //      }
    ???

  //  }

  private def selectOrInsertUserInCourse(user: User, course: Course): Future[Boolean] =
    tableDefs.futureUserIsRegisteredForCourse(user.username, course.id).flatMap {
      case true  => Future.successful(true)
      case false => tableDefs.futureRegisterUserForCourse(user.username, course.id)
    }


  def lti: Action[AnyContent] = Action.async { implicit request =>
    def onError: Form[LtiFormValues] => Future[Result] = { formWithErrors =>
      formWithErrors.errors.foreach(println)
      Future(BadRequest("The form was not valid!"))
    }

    def onRead: LtiFormValues => Future[Result] = { ltiFormValues =>

      for {
        user <- selectOrInsertUser(ltiFormValues.username)
        course <- selectOrInsertCourse(ltiFormValues.courseIdentifier, ltiFormValues.courseName)
        maybePw <- tableDefs.futurePwHashForUser(user)
        _ <- selectOrInsertUserInCourse(user, course)
      } yield {
        val baseRedirect = Redirect(routes.HomeController.index()).withSession(idName -> user.username)

        if (maybePw.isEmpty) baseRedirect.flashing("no_pw_set" -> maybePw.isDefined.toString)
        else baseRedirect
      }

    }

    FormMappings.ltiValuesForm.bindFromRequest().fold(onError, onRead)

  }

  def registerAsLtiProvider: Action[AnyContent] = Action { implicit request =>

    def onError: Form[LtiToolProxyRegistrationRequestFormValues] => Result = { formWithErrors =>
      formWithErrors.errors.foreach(println)
      BadRequest("TODO!")
    }

    def onRead: LtiToolProxyRegistrationRequestFormValues => Result = { ltiToolProxyRegistrationRequestFormValues =>
      println(ltiToolProxyRegistrationRequestFormValues)

      ???
    }

    FormMappings.ltiToolProxyRegistrationRequestForm.bindFromRequest.fold(onError, onRead)
  }
}
