package controllers

import model._
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

trait ControllerHelpers extends Secured {
  self: AbstractController =>

  protected val tableDefs: TableDefs

  private def onNoSuchLanguage(langId: Int): Result = NotFound(s"Es gibt keine Sprache mit der ID $langId")

  private def onNoSuchCollection(language: Language, collId: Int): Result =
    NotFound(s"Es gibt keine Sammlung mit der ID $collId für die Sprache ${language.name}")

  private def onNuSuchFlashcard(language: Language, collection: Collection, cardId: Int): Result =
    NotFound(s"Es gibt keine Karteikarte mit der ID $cardId für die Sammlung ${collection.name} für die Sprache ${language.name}")

  protected def futureWithUserAndLanguage(langId: Int)(f: (User, Language) => Request[AnyContent] => Future[Result])
                                       (implicit ec: ExecutionContext): EssentialAction =
    futureWithUser { user =>
      implicit request =>
        tableDefs.futureLanguageById(langId) flatMap {
          case None           => Future(onNoSuchLanguage(langId))
          case Some(language) => f(user, language)(request)
        }
    }

  protected def futureWithUserAndCollection(langId: Int, collId: Int)(f: (User, Language, Collection) => Request[AnyContent] => Future[Result])
                                         (implicit ec: ExecutionContext): EssentialAction =
    futureWithUserAndLanguage(langId) { (user, language) =>
      implicit request =>
        tableDefs.futureCollectionById(language, collId) flatMap {
          case None             => Future(onNoSuchCollection(language, collId))
          case Some(collection) => f(user, language, collection)(request)
        }
    }

  protected def withUserAndCompleteFlashcard(langId: Int, collId: Int, cardId: Int)
                                          (f: (User, Language, Collection, CompleteFlashcard) => Request[AnyContent] => Result)
                                          (implicit ec: ExecutionContext): EssentialAction =
    futureWithUserAndCollection(langId, collId) { (user, language, collection) =>
      implicit request =>
        tableDefs.futureFlashcardById(collection, cardId) flatMap {
          case None            => Future(onNuSuchFlashcard(language, collection, cardId))
          case Some(flashcard) => tableDefs.futureChoiceAnswersForFlashcard(flashcard) map {
            answers => f(user, language, collection, CompleteFlashcard(flashcard, answers))(request)
          }
        }
    }

  protected def futureWithUserAndCompleteFlashcard(langId: Int, collId: Int, cardId: Int)
                                                (f: (User, Language, Collection, CompleteFlashcard) => Request[AnyContent] => Future[Result])
                                                (implicit ec: ExecutionContext): EssentialAction =
    futureWithUserAndCollection(langId, collId) { (user, language, collection) =>
      implicit request =>
        tableDefs.futureFlashcardById(collection, cardId) flatMap {
          case None            => Future(onNuSuchFlashcard(language, collection, cardId))
          case Some(flashcard) => tableDefs.futureChoiceAnswersForFlashcard(flashcard) flatMap {
            answers => f(user, language, collection, CompleteFlashcard(flashcard, answers))(request)
          }
        }
    }


}
