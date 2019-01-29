package controllers

import com.github.t3hnar.bcrypt._
import javax.inject._
import model.RegisterFormValues
import model.Consts._
import model._
import play.api.data.Form
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.mvc._
import slick.jdbc.JdbcProfile

import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps

class LoginController @Inject()(cc: ControllerComponents, val dbConfigProvider: DatabaseConfigProvider, val tableDefs: TableDefs)(implicit ec: ExecutionContext)
  extends AbstractController(cc) with HasDatabaseConfigProvider[JdbcProfile] with play.api.i18n.I18nSupport {

  def registerForm: Action[AnyContent] = Action {
    implicit request => Ok(views.html.forms.registerForm(FormMappings.registerValuesForm))
  }

  def register: Action[AnyContent] = Action.async { implicit request =>

    val onError: Form[RegisterFormValues] => Future[Result] = { _ =>
      Future(BadRequest("There has been an error in your form..."))
    }

    val onRead: RegisterFormValues => Future[Result] = { credentials =>
      val newUser = User(credentials.username, credentials.name)
      val pwHash = UserPassword(credentials.username, credentials.pw.bcrypt)

      tableDefs.futureSaveUser(newUser) flatMap {
        case false => Future(BadRequest("Could not save user!"))
        case true  => tableDefs.savePwHash(pwHash) map {
          _ => Redirect(routes.LoginController.loginForm())
          //              Ok(views.html.registered.render(credentials.username))
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