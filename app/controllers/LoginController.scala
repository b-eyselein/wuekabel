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

class LoginController @Inject()(cc: ControllerComponents, val dbConfigProvider: DatabaseConfigProvider, val tableDefs: TableDefs)(override implicit val ec: ExecutionContext)
  extends AbstractController(cc) with HasDatabaseConfigProvider[JdbcProfile] with play.api.i18n.I18nSupport with Secured {

  override protected val adminRightsRequired: Boolean = false

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
        case Some(user) => tableDefs.futurePwHashForUser(user) map {
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

  def changePwForm: EssentialAction = futureWithUser { user =>
    implicit request =>
      tableDefs.futurePwHashForUser(user).map(_.isDefined) map {
        hasPw => Ok(views.html.forms.changePwForm(user, FormMappings.changePwForm, hasPw))
      }
  }

  def changePw: EssentialAction = futureWithUser { user =>
    implicit request =>

      tableDefs.futurePwHashForUser(user) flatMap { maybePwHashForUser: Option[UserPassword] =>

        val hasPw = maybePwHashForUser.isDefined

        def onError: Form[ChangePwFormValues] => Future[Result] = { formWithErrors =>
          Future.successful(BadRequest(views.html.forms.changePwForm(user, formWithErrors, hasPw)))
        }

        def onRead: ChangePwFormValues => Future[Result] = { changePwFormValues: ChangePwFormValues =>

          val newPwsEqual = changePwFormValues.firstNewPw == changePwFormValues.secondNewPw

          val oldPwCorrect = maybePwHashForUser.map(_.pwHash) match {
            case None            => true
            case Some(oldPwHash) => changePwFormValues.oldPw.exists(oldPw => oldPw.isBcrypted(oldPwHash))
          }

          if (oldPwCorrect && newPwsEqual) {
            println(changePwFormValues)

            tableDefs.futureUpdatePwHashForUser(user, changePwFormValues.firstNewPw.bcrypt) map {
              case false => ???
              case true  => Redirect(routes.HomeController.index())
            }
          } else {
            Future.successful(BadRequest(views.html.forms.changePwForm(user, FormMappings.changePwForm.fill(changePwFormValues), hasPw)))
          }
        }

        FormMappings.changePwForm.bindFromRequest.fold(onError, onRead)
      }
  }

}
