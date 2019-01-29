package controllers

import javax.inject.{Inject, Singleton}
import model._
import play.api.libs.json.{JsError, JsSuccess}
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class HomeController @Inject()(cc: ControllerComponents, protected val tableDefs: TableDefs)(implicit ec: ExecutionContext)
  extends AbstractController(cc) with Secured with play.api.i18n.I18nSupport {

  // Helper methods

  private def onNoSuchLanguage(langId: Int): Result = NotFound(s"Es gibt keine Sprache mit der ID $langId")

  private def onNoSuchCollection(language: Language, collId: Int): Result =
    NotFound(s"Es gibt keine Sammlung mit der ID $collId für die Sprache ${language.name}")

  private def onNuSuchFlashcard(language: Language, collection: Collection, cardId: Int): Result =
    NotFound(s"Es gibt keine Karteikarte mit der ID $cardId für die Sammlung ${collection.name} für die Sprache ${language.name}")

  private def futureWithUserAndLanguage(langId: Int)(f: (User, Language) => Request[AnyContent] => Future[Result]): EssentialAction =
    futureWithUser { user =>
      implicit request =>
        tableDefs.futureLanguageById(langId) flatMap {
          case None           => Future(onNoSuchLanguage(langId))
          case Some(language) => f(user, language)(request)
        }
    }

  //  private def withUserLanguageAndCollection(langId: Int, collId: Int)(f: (User, Language, Collection) => Request[AnyContent] => Result): EssentialAction =
  //    futureWithUserAndLanguage(langId) { (user, language) =>
  //      implicit request =>
  //        tableDefs.futureCollectionById(language, collId) map {
  //          case None             => onNoSuchCollection(language, collId)
  //          case Some(collection) => f(user, language, collection)(request)
  //        }
  //    }

  private def futureWithUserAndCollection(langId: Int, collId: Int)(f: (User, Language, Collection) => Request[AnyContent] => Future[Result]): EssentialAction =
    futureWithUserAndLanguage(langId) { (user, language) =>
      implicit request =>
        tableDefs.futureCollectionById(language, collId) flatMap {
          case None             => Future(onNoSuchCollection(language, collId))
          case Some(collection) => f(user, language, collection)(request)
        }
    }

  private def withUserAndCompleteFlashcard(langId: Int, collId: Int, cardId: Int)
                                          (f: (User, Language, Collection, CompleteFlashcard) => Request[AnyContent] => Result): EssentialAction =
    futureWithUserAndCollection(langId, collId) { (user, language, collection) =>
      implicit request =>
        tableDefs.futureFlashcardById(collection, cardId) flatMap {
          case None            => Future(onNuSuchFlashcard(language, collection, cardId))
          case Some(flashcard) => tableDefs.futureChoiceAnswersForFlashcard(flashcard) map {
            answers => f(user, language, collection, CompleteFlashcard(flashcard, answers))(request)
          }
        }
    }

  private def futureWithUserAndCompleteFlashcard(langId: Int, collId: Int, cardId: Int)
                                                (f: (User, Language, Collection, CompleteFlashcard) => Request[AnyContent] => Future[Result]): EssentialAction =
    futureWithUserAndCollection(langId, collId) { (user, language, collection) =>
      implicit request =>
        tableDefs.futureFlashcardById(collection, cardId) flatMap {
          case None            => Future(onNuSuchFlashcard(language, collection, cardId))
          case Some(flashcard) => tableDefs.futureChoiceAnswersForFlashcard(flashcard) flatMap {
            answers => f(user, language, collection, CompleteFlashcard(flashcard, answers))(request)
          }
        }
    }

  // Routes

  def index: EssentialAction = futureWithUser { user =>
    implicit request =>
      tableDefs.futureLanguagesForUser(user) map { languages =>
        Ok(views.html.myLanguages(user, languages))
      }
  }

  def allLanguages: EssentialAction = futureWithUser { user =>
    implicit request =>
      tableDefs.futureLanguagesAndUserLearns(user) map { languagesAndUserLearns =>
        Ok(views.html.allLanguages(user, languagesAndUserLearns))
      }
  }

  def language(langId: Int): EssentialAction = futureWithUserAndLanguage(langId) { (user, language) =>
    implicit request =>
      tableDefs.futureCollectionsForLanguage(language) map {
        collections => Ok(views.html.language(user, language, collections))
      }
  }

  def selectLanguage(langId: Int): EssentialAction = futureWithUserAndLanguage(langId) { (user, language) =>
    implicit request =>
      tableDefs.activateLanguageForUser(user, language) map {
        case true  => Redirect(routes.HomeController.allLanguages())
        case false => ???
      }
  }

  def deselectLanguage(langId: Int): EssentialAction = futureWithUserAndLanguage(langId) { (user, language) =>
    implicit request =>
      tableDefs.deactivateLanguageForUser(user, language) map {
        case true  => Redirect(routes.HomeController.allLanguages())
        case false => ???
      }
  }

  def collection(langId: Int, collId: Int): EssentialAction = futureWithUserAndCollection(langId, collId) { (user, language, collection) =>
    implicit request =>
      for {
        flashcardCount <- tableDefs.futureFlashcardCountForCollection(collection)
        toLearnCount <- tableDefs.futureFlashcardsToLearnCount(user, collection)
        toRepeatCount <- tableDefs.futureFlashcardsToRepeatCount(user, collection)
      } yield Ok(views.html.collection(user, language, collection, flashcardCount, toLearnCount, toRepeatCount))
  }

  def startLearning(langId: Int, collId: Int): EssentialAction = futureWithUserAndCollection(langId, collId) { (user, _, collection) =>
    implicit request =>
      tableDefs.futureMaybeIdentifierNextFlashcardToLearn(user, collection) map {
        case None             => ???
        case Some(identifier) => Redirect(routes.HomeController.learn(identifier.langId, identifier.collId, identifier.cardId))
      }
  }

  def learn(langId: Int, collId: Int, cardId: Int): EssentialAction = withUserAndCompleteFlashcard(langId, collId, cardId) { (user, language, collection, completeFlashcard) =>
    implicit request => Ok(views.html.learn(user, completeFlashcard))
  }

  def startRepeating(langId: Int, collId: Int): EssentialAction = futureWithUserAndCollection(langId, collId) { (_, _, _) =>
    implicit request =>
      ???
  }

  def repeat(langId: Int, collId: Int, cardId: Int): EssentialAction = withUserAndCompleteFlashcard(langId, collId, cardId) { (_, _, _, _) =>
    implicit request =>
      ???
  }

  def checkSolution(langId: Int, collId: Int, cardId: Int): EssentialAction = futureWithUserAndCompleteFlashcard(langId, collId, cardId) { (user, _, _, completeFlashcard) =>
    implicit request =>
      request.body.asJson match {
        case None       => ???
        case Some(json) => JsonFormats.solutionFormat.reads(json) match {
          case JsError(_)             => ???
          case JsSuccess(solution, _) =>
            val correctionResult = Corrector.correct(completeFlashcard, solution)

            tableDefs.futureInsertOrUpdateUserAnswer(user, completeFlashcard.flashcard, correctionResult.correct) map {
              _ => Ok(JsonFormats.correctionResultWrites.writes(correctionResult))
            }
        }
      }
  }

}
