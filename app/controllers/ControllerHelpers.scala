package controllers

import model._
import model.persistence.TableDefs
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

trait ControllerHelpers extends Secured {
  self: AbstractController =>

  protected implicit val ec: ExecutionContext

  protected val tableDefs: TableDefs

  private def onNoSuchCourse(courseId: String): Result = NotFound(s"Es gibt keinen Kurs mit der ID '$courseId'")

  private def onNoSuchLanguage(langId: Int): Result = NotFound(s"Es gibt keine Sprache mit der ID '$langId'")

  private def onNoSuchCollection(collId: Int): Result = NotFound(s"Es gibt keine Sammlung mit der ID '$collId'")

  private def onNuSuchFlashcard(collection: Collection, cardId: Int): Result =
    NotFound(s"Es gibt keine Karteikarte mit der ID '$cardId' für die Sammlung '${collection.name}'!")

  protected def withUserAndLanguage(adminRightsRequired: Boolean, langId: Int)
                                   (f: (User, Language) => Request[AnyContent] => Result): EssentialAction =
    futureWithUser(adminRightsRequired) { user =>
      implicit request =>
        tableDefs.futureLanguageById(langId) map {
          case None           => onNoSuchLanguage(langId)
          case Some(language) => f(user, language)(request)
        }
    }

  protected def futureWithUserAndLanguage(adminRightsRequired: Boolean, langId: Int)
                                         (f: (User, Language) => Request[AnyContent] => Future[Result]): EssentialAction =
    futureWithUser(adminRightsRequired) { user =>
      implicit request =>
        tableDefs.futureLanguageById(langId) flatMap {
          case None           => Future.successful(onNoSuchLanguage(langId))
          case Some(language) => f(user, language)(request)
        }
    }

  protected def withUserAndCourse(adminRightsRequired: Boolean, courseId: String)
                                 (f: (User, Course) => Request[AnyContent] => Result): EssentialAction =
    futureWithUser(adminRightsRequired) { user =>
      implicit request =>
        tableDefs.futureCourseById(courseId) map {
          case None         => onNoSuchCourse(courseId)
          case Some(course) => f(user, course)(request)
        }
    }

  protected def futureWithUserAndCourse(adminRightsRequired: Boolean, courseId: String)
                                       (f: (User, Course) => Request[AnyContent] => Future[Result]): EssentialAction =
    futureWithUser(adminRightsRequired) { user =>
      implicit request =>
        tableDefs.futureCourseById(courseId) flatMap {
          case None         => Future.successful(onNoSuchCourse(courseId))
          case Some(course) => f(user, course)(request)
        }
    }


  protected def withUserAndCollection(adminRightsRequired: Boolean, collId: Int)
                                     (f: (User, Collection) => Request[AnyContent] => Result): EssentialAction =
    futureWithUser(adminRightsRequired) { user =>
      implicit request =>
        tableDefs.futureCollectionById(collId) map {
          case None             => onNoSuchCollection(collId)
          case Some(collection) => f(user, collection)(request)
        }
    }


  protected def futureWithUserAndCollection(adminRightsRequired: Boolean, collId: Int)
                                           (f: (User, Collection) => Request[AnyContent] => Future[Result]): EssentialAction =
    futureWithUser(adminRightsRequired) { user =>
      implicit request =>
        tableDefs.futureCollectionById(collId) flatMap {
          case None             => Future.successful(onNoSuchCollection(collId))
          case Some(collection) => f(user, collection)(request)
        }
    }

  protected def withUserAndCompleteFlashcard(adminRightsRequired: Boolean, langId: Int, collId: Int, cardId: Int)
                                            (f: (User, Collection, Flashcard) => Request[AnyContent] => Result): EssentialAction =
    futureWithUserAndCollection(adminRightsRequired, collId) { (user, collection) =>
      implicit request =>
        tableDefs.futureFlashcardById(collection, cardId) map {
          case None            => onNuSuchFlashcard(collection, cardId)
          case Some(flashcard) => f(user, collection, flashcard)(request)
        }
    }

  protected def futureWithUserAndCompleteFlashcard(adminRightsRequired: Boolean, collId: Int, cardId: Int)
                                                  (f: (User, Collection, Flashcard) => Request[AnyContent] => Future[Result]): EssentialAction =
    futureWithUserAndCollection(adminRightsRequired, collId) { (user, collection) =>
      implicit request =>
        tableDefs.futureFlashcardById(collection, cardId) flatMap {
          case None            => Future.successful(onNuSuchFlashcard(collection, cardId))
          case Some(flashcard) => f(user, collection, flashcard)(request)
        }
    }


}
