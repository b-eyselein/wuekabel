package controllers

import com.github.t3hnar.bcrypt._
import javax.inject._
import model.RegisterFormValues
import model.Consts._
import model._
import model.persistence.TableDefs
import play.api.data.Form
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.mvc._
import slick.jdbc.JdbcProfile

import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps

class LoginController @Inject()(cc: ControllerComponents, val dbConfigProvider: DatabaseConfigProvider, val tableDefs: TableDefs)(implicit ec: ExecutionContext)
  extends AbstractController(cc) with HasDatabaseConfigProvider[JdbcProfile] with play.api.i18n.I18nSupport {

  def lti: Action[AnyContent] = Action.async { implicit request =>
    request.body.asFormUrlEncoded match {
      case None       => Future(BadRequest("Body did not contain awaited values..."))
      case Some(data) =>

        def onError: Form[LtiFormValues] => Future[Result] = { formWithErrors =>
          formWithErrors.errors.foreach(println)
          Future(BadRequest("The form was not valid!"))
        }

        def onRead: LtiFormValues => Future[Result] = { ltiFormValues =>

          for {
            user <- selectOrInsertUser(ltiFormValues.username)
            course <- selectOrInsertCourse(ltiFormValues.courseIdentifier, ltiFormValues.courseName)
            _ <- selectOrInsertUserInCourse(user, course)
          } yield Redirect(routes.HomeController.index()).withSession(idName -> user.username)

        }

        FormMappings.ltiValuesForm.bindFromRequest().fold(onError, onRead)
    }
  }

  private def selectOrInsertUser(username: String): Future[User] = tableDefs.futureUserByUserName(username) flatMap {
    case Some(user) => Future.successful(user)
    case None       =>
      val newUser = User(username)
      tableDefs.futureInsertUser(newUser) map {
        case true  => newUser
        case false => ???
      }
  }

  private def selectOrInsertCourse(id: String, name: String): Future[Course] = tableDefs.futureCourseById(id) flatMap {
    case Some(course) => Future.successful(course)
    case None         =>
      val newCourse = Course(id, name)
      tableDefs.futureInsertCourse(newCourse) map {
        case true  => newCourse
        case false => ???
      }
  }

  private def selectOrInsertUserInCourse(user: User, course: Course): Future[Boolean] = tableDefs.futureUserInCourse(user, course) flatMap {
    case true  => Future.successful(true)
    case false => tableDefs.futureAddUserToCourse(user, course)
  }

  def registerForm: Action[AnyContent] = Action {
    implicit request => Ok(views.html.forms.registerForm(FormMappings.registerValuesForm))
  }

  def register: Action[AnyContent] = Action.async { implicit request =>

    val onError: Form[RegisterFormValues] => Future[Result] = { _ =>
      Future(BadRequest("There has been an error in your form..."))
    }

    val onRead: RegisterFormValues => Future[Result] = { credentials =>
      val newUser = User(credentials.username)
      val pwHash = UserPassword(credentials.username, credentials.pw.bcrypt)

      tableDefs.futureInsertUser(newUser) flatMap {
        case false => Future(BadRequest("Could not save user!"))
        case true  => tableDefs.futureSavePwHash(pwHash) map {
          _ => Redirect(routes.LoginController.loginForm())
        }
      }
    }

    FormMappings.registerValuesForm.bindFromRequest.fold(onError, onRead)
  }

  def login: Action[AnyContent] = Action.async { implicit request =>

    val onError: Form[LoginFormValues] => Future[Result] = { formWithErrors =>
      Future(BadRequest(views.html.forms.loginForm(formWithErrors)))
    }

    val onRead: LoginFormValues => Future[Result] = { credentials =>

      tableDefs.futureUserByUserName(credentials.username) flatMap {
        case None       => Future(Redirect(controllers.routes.LoginController.registerForm()))
        case Some(user) => tableDefs.futurePwHashForUser(credentials.username) map {
          case None               => BadRequest("Cannot change password!")
          case Some(userPassword) =>
            if (credentials.password isBcrypted userPassword.pwHash) {
              Redirect(controllers.routes.HomeController.index()).withSession(idName -> user.username)
            } else {
              Ok(views.html.forms.loginForm(FormMappings.loginValuesForm.fill(credentials)))
            }
        }
      }
    }

    FormMappings.loginValuesForm.bindFromRequest.fold(onError, onRead)
  }

  def loginForm: Action[AnyContent] = Action {
    implicit request => Ok(views.html.forms.loginForm(FormMappings.loginValuesForm))
  }

  def logout: Action[AnyContent] = Action {
    implicit request => Redirect(routes.LoginController.loginForm()).withNewSession
  }

}