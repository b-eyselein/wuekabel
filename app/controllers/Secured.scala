package controllers

import model.Consts.idName
import model.User
import model.persistence.TableDefs
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

trait Secured {
  self: AbstractController =>

  implicit protected val ec: ExecutionContext

  protected val tableDefs: TableDefs

  protected val adminRightsRequired: Boolean

  private def username(request: RequestHeader): Option[String] = request.session.get(idName)

  private def onUnauthorized: Result = Redirect(controllers.routes.LoginController.loginForm).withNewSession

  private def futureOnUnauthorized: Future[Result] = Future.successful(onUnauthorized)

  private def withAuth(f: => String => Request[AnyContent] => Future[Result]): EssentialAction =
    Security.Authenticated(username, _ => onUnauthorized)(user =>
      controllerComponents.actionBuilder.async(request => f(user)(request))
    )

  protected def withUser(f: User => Request[AnyContent] => Result): EssentialAction = withAuth {
    username => implicit request =>
      tableDefs.futureUserByUserName(username) map {
        case None       => onUnauthorized
        case Some(user) => if (!adminRightsRequired || user.isAdmin) f(user)(request) else onUnauthorized
      }
  }

  protected def futureWithUser(f: User => Request[AnyContent] => Future[Result]): EssentialAction = withAuth {
    username => implicit request =>
      tableDefs.futureUserByUserName(username) flatMap {
        case None       => futureOnUnauthorized
        case Some(user) => if (!adminRightsRequired || user.isAdmin) f(user)(request) else futureOnUnauthorized
      }
  }

}
