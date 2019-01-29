package controllers

import model.User
import model.Consts.idName
import model.TableDefs
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

trait Secured {
  self: AbstractController =>

  protected val tableDefs: TableDefs

  private def username(request: RequestHeader): Option[String] = request.session.get(idName)

  private def onUnauthorized(request: RequestHeader): Result = Redirect(controllers.routes.LoginController.loginForm()).withNewSession

  private def futureOnUnauthorized(request: RequestHeader)(implicit ec: ExecutionContext): Future[Result] =
    Future(onUnauthorized(request))

  private def withAuth(f: => String => Request[AnyContent] => Future[Result]): EssentialAction =
    Security.Authenticated(username, onUnauthorized)(user => controllerComponents.actionBuilder.async(request => f(user)(request)))

  private def withAuthWithBodyParser[A](bodyParser: BodyParser[A])(f: => String => Request[A] => Future[Result]): EssentialAction =
    Security.Authenticated(username, onUnauthorized)(user => controllerComponents.actionBuilder.async(bodyParser)(request => f(user)(request)))


  def withUser(f: User => Request[AnyContent] => Result)(implicit ec: ExecutionContext): EssentialAction = withAuth { username =>
    implicit request => {
      tableDefs.futureUserByUserName(username) map {
        case Some(user) => f(user)(request)
        case None       => onUnauthorized(request)
      }
    }
  }

  def futureWithUser(f: User => Request[AnyContent] => Future[Result])(implicit ec: ExecutionContext): EssentialAction = withAuth { username =>
    implicit request =>
      tableDefs.futureUserByUserName(username) flatMap {
        case Some(user) => f(user)(request)
        case None       => futureOnUnauthorized(request)
      }

  }

  def futureWithUserWithBodyParser[A](bodyParser: BodyParser[A])(f: User => Request[A] => Future[Result])(implicit ec: ExecutionContext): EssentialAction =
    withAuthWithBodyParser(bodyParser) { username =>
      implicit request =>
        tableDefs.futureUserByUserName(username) flatMap {
          case Some(user) => f(user)(request)
          case None       => futureOnUnauthorized(request)
        }
    }

}