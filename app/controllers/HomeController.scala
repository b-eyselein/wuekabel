package controllers

import javax.inject.{Inject, Singleton}
import model._
import play.api.libs.json.{JsError, JsSuccess}
import play.api.mvc._

import scala.concurrent.ExecutionContext

@Singleton
class HomeController @Inject()(cc: ControllerComponents, protected val tableDefs: TableDefs)(implicit protected val ec: ExecutionContext)
  extends AbstractController(cc) with ControllerHelpers with play.api.i18n.I18nSupport {

  // Routes

  def index: EssentialAction = futureWithUser(adminRightsRequired = false) { user =>
    implicit request =>
      tableDefs.futureLanguagesForUser(user) map { languages =>
        Ok(views.html.myLanguages(user, languages))
      }
  }

  def allLanguages: EssentialAction = futureWithUser(adminRightsRequired = false) { user =>
    implicit request =>
      tableDefs.futureLanguagesAndUserLearns(user) map { languagesAndUserLearns =>
        Ok(views.html.allLanguages(user, languagesAndUserLearns))
      }
  }

  def language(langId: Int): EssentialAction =
    futureWithUserAndLanguage(adminRightsRequired = false, langId) { (user, language) =>
      implicit request =>
        tableDefs.futureCollectionsForLanguage(language) map {
          collections => Ok(views.html.language(user, language, collections))
        }
    }

  def selectLanguage(langId: Int): EssentialAction =
    futureWithUserAndLanguage(adminRightsRequired = false, langId) { (user, language) =>
      implicit request =>
        tableDefs.activateLanguageForUser(user, language) map {
          case true  => Redirect(routes.HomeController.allLanguages())
          case false => ???
        }
    }

  def deselectLanguage(langId: Int): EssentialAction =
    futureWithUserAndLanguage(adminRightsRequired = false, langId) { (user, language) =>
      implicit request =>
        tableDefs.deactivateLanguageForUser(user, language) map {
          case true  => Redirect(routes.HomeController.allLanguages())
          case false => ???
        }
    }

  def collection(langId: Int, collId: Int): EssentialAction =
    futureWithUserAndCollection(adminRightsRequired = false, langId, collId) { (user, language, collection) =>
      implicit request =>
        for {
          flashcardCount <- tableDefs.futureFlashcardCountForCollection(collection)
          toLearnCount <- tableDefs.futureFlashcardsToLearnCount(user, collection)
          toRepeatCount <- tableDefs.futureFlashcardsToRepeatCount(user, collection)
        } yield Ok(views.html.collection(user, language, collection, flashcardCount, toLearnCount, toRepeatCount))
    }


  def startLearning(langId: Int, collId: Int): EssentialAction =
    futureWithUserAndCollection(adminRightsRequired = false, langId, collId) { (user, _, collection) =>
      implicit request =>
        tableDefs.futureMaybeIdentifierNextFlashcardToLearn(user, collection) map {
          case None             => Redirect(routes.HomeController.collection(langId, collId))
          case Some(identifier) => Redirect(routes.HomeController.learn(identifier.langId, identifier.collId, identifier.cardId))
        }
    }

  def learn(langId: Int, collId: Int, cardId: Int): EssentialAction =
    withUserAndCompleteFlashcard(adminRightsRequired = false, langId, collId, cardId) { (user, _, _, completeFlashcard) =>
      implicit request => Ok(views.html.learn(user, completeFlashcard, isRepeating = false))
    }

  def startRepeating(langId: Int, collId: Int): EssentialAction =
    futureWithUserAndCollection(adminRightsRequired = false, langId, collId) { (user, _, collection) =>
      implicit request =>
        tableDefs.futureMaybeIdentifierNextFlashcardToRepeat(user, collection) map {
          case None             => Redirect(routes.HomeController.collection(langId, collId))
          case Some(identifier) => Redirect(routes.HomeController.repeat(identifier.langId, identifier.collId, identifier.cardId))
        }
    }

  def repeat(langId: Int, collId: Int, cardId: Int): EssentialAction =
    withUserAndCompleteFlashcard(adminRightsRequired = false, langId, collId, cardId) { (user, _, _, completeFlashcard) =>
      implicit request => Ok(views.html.learn(user, completeFlashcard, isRepeating = false))
    }

  def checkSolution(langId: Int, collId: Int, cardId: Int): EssentialAction =
    futureWithUserAndCompleteFlashcard(adminRightsRequired = false, langId, collId, cardId) { (user, _, _, completeFlashcard) =>
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
