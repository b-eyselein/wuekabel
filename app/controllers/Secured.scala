package controllers

import model.User
import model.Consts.idName
import model.persistence.TableDefs
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

trait Secured {
  self: AbstractController =>

  implicit protected val ec: ExecutionContext

  protected val tableDefs: TableDefs

  private def username(request: RequestHeader): Option[String] = request.session.get(idName)

  private def onUnauthorized(request: RequestHeader): Result = Redirect(controllers.routes.LoginController.loginForm()).withNewSession

  private def futureOnUnauthorized(request: RequestHeader): Future[Result] =
    Future(onUnauthorized(request))

  private def withAuth(f: => String => Request[AnyContent] => Future[Result]): EssentialAction =
    Security.Authenticated(username, onUnauthorized)(user => controllerComponents.actionBuilder.async(request => f(user)(request)))

  protected def withUser(adminRightsRequired: Boolean)(f: User => Request[AnyContent] => Result): EssentialAction = withAuth { username =>
    implicit request => {
      tableDefs.futureUserByUserName(username) map {
        case None       => onUnauthorized(request)
        case Some(user) =>
          if (!adminRightsRequired || user.isAdmin) f(user)(request)
          else onUnauthorized(request)
      }
    }
  }

  protected def futureWithUser(adminRightsRequired: Boolean)(f: User => Request[AnyContent] => Future[Result]): EssentialAction = withAuth { username =>
    implicit request =>
      tableDefs.futureUserByUserName(username) flatMap {
        case None       => futureOnUnauthorized(request)
        case Some(user) =>
          if (!adminRightsRequired || user.isAdmin) f(user)(request)
          else futureOnUnauthorized(request)
      }
  }

}