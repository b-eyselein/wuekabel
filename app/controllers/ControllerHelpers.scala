package controllers

import model._
import model.persistence.TableDefs
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

trait ControllerHelpers extends Secured {
  self: AbstractController =>

  protected implicit val ec: ExecutionContext

  protected val tableDefs: TableDefs

  private def onNoSuchCourse(courseId: Int): Result = NotFound(s"Es gibt keinen Kurs mit der ID '$courseId'")

  private def onNoSuchCollection(courseId: Int, collId: Int): Result = NotFound(s"Es gibt keine Sammlung mit der ID '$collId' für den Kurs '$courseId'")

  private def onNuSuchFlashcard(collection: Collection, cardId: Int): Result =
    NotFound(s"Es gibt keine Karteikarte mit der ID '$cardId' für die Sammlung '${collection.name}'!")

  protected def withUserAndCourse(courseId: Int)(f: (User, Course) => Request[AnyContent] => Result): EssentialAction =
    futureWithUser { user =>
      implicit request =>
        tableDefs.futureCourseById(courseId) map {
          case None         => onNoSuchCourse(courseId)
          case Some(course) => f(user, course)(request)
        }
    }

  protected def futureWithUserAndCourse(courseId: Int)(f: (User, Course) => Request[AnyContent] => Future[Result]): EssentialAction =
    futureWithUser { user =>
      implicit request =>
        tableDefs.futureCourseById(courseId) flatMap {
          case None         => Future.successful(onNoSuchCourse(courseId))
          case Some(course) => f(user, course)(request)
        }
    }


  protected def futureWithUserAndCollection(courseId: Int, collId: Int)(f: (User, Collection) => Request[AnyContent] => Future[Result]): EssentialAction =
    futureWithUser { user =>
      implicit request =>
        tableDefs.futureCollectionById(courseId, collId) flatMap {
          case None             => Future.successful(onNoSuchCollection(courseId, collId))
          case Some(collection) => f(user, collection)(request)
        }
    }

  protected def futureWithUserAndCompleteFlashcard(courseId: Int, collId: Int, cardId: Int)(f: (User, Collection, Flashcard) => Request[AnyContent] => Future[Result]): EssentialAction =
    futureWithUserAndCollection(courseId, collId) { (user, collection) =>
      implicit request =>
        tableDefs.futureFlashcardById(collection, cardId) flatMap {
          case None            => Future.successful(onNuSuchFlashcard(collection, cardId))
          case Some(flashcard) => f(user, collection, flashcard)(request)
        }
    }


}
