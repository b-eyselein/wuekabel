package controllers

import model._
import model.persistence.TableDefs
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

trait NewControllerHelpers extends Secured {
  self: AbstractController =>

  protected implicit val ec: ExecutionContext

  protected val tableDefs: TableDefs

  protected def onNoSuchCourse(user: User, courseId: Int): Result =
    NotFound(views.html.errorViews.noSuchCourse(user, courseId))

  protected def onNoSuchCollection(user: User, course: Course, collId: Int): Result =
    NotFound(views.html.errorViews.noSuchCollection(user, course, collId))

  protected def onNoSuchFlashcard(user: User, course: Course, collection: Collection, cardId: Int): Result =
    NotFound(views.html.errorViews.noSuchFlashcard(user, course, collection, cardId))

  protected def withUserAndCourse(courseId: Int)(f: (User, Course) => Request[AnyContent] => Result): EssentialAction =
    futureWithUser { user =>
      implicit request =>
        tableDefs.futureCourseById(courseId).map {
          case None         => onNoSuchCourse(user, courseId)
          case Some(course) => f(user, course)(request)
        }
    }

  protected def futureWithUserAndCourse(courseId: Int)(f: (User, Course) => Request[AnyContent] => Future[Result]): EssentialAction =
    futureWithUser { user =>
      implicit request =>
        tableDefs.futureCourseById(courseId).flatMap {
          case None         => Future.successful(onNoSuchCourse(user, courseId))
          case Some(course) => f(user, course)(request)
        }
    }


  protected def withUserAndCollection(courseId: Int, collId: Int)(f: (User, Course, Collection) => Request[AnyContent] => Result): EssentialAction =
    futureWithUser { user =>
      implicit request =>
        tableDefs.futureCourseById(courseId).flatMap {
          case None         => Future.successful(onNoSuchCourse(user, courseId))
          case Some(course) =>

            tableDefs.futureCollectionById(courseId, collId).map {
              case None             => onNoSuchCollection(user, course, collId)
              case Some(collection) => f(user, course, collection)(request)
            }
        }
    }

  protected def futureWithUserAndCollection(courseId: Int, collId: Int)(f: (User, Course, Collection) => Request[AnyContent] => Future[Result]): EssentialAction =
    futureWithUser { user =>
      implicit request =>
        tableDefs.futureCourseById(courseId).flatMap {
          case None         => Future.successful(onNoSuchCourse(user, courseId))
          case Some(course) =>

            tableDefs.futureCollectionById(courseId, collId).flatMap {
              case None             => Future.successful(onNoSuchCollection(user, course, collId))
              case Some(collection) => f(user, course, collection)(request)
            }
        }
    }

}
