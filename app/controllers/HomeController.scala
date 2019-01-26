package controllers

import javax.inject.{Inject, Singleton}
import model.{Language, TableDefs}
import play.api.mvc.{AbstractController, ControllerComponents, EssentialAction, Result}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class HomeController @Inject()(cc: ControllerComponents, protected val tableDefs: TableDefs)(implicit ec: ExecutionContext)
  extends AbstractController(cc) with Secured {

  private def onNoSuchLanguage(langId: Int): Result = NotFound(s"Es gibt keine Sprache mit der ID $langId")

  private def onNoSuchCollection(language: Language, collId: Int): Result =
    NotFound(s"Es gibt keine Sammlung mit der ID $collId für die Sprache ${language.name}")

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

  def language(langId: Int): EssentialAction = futureWithUser { user =>
    implicit request =>
      tableDefs.futureLanguageById(langId) flatMap {
        case None           => Future(onNoSuchLanguage(langId))
        case Some(language) => tableDefs.futureCollectionsForLanguage(language) map {
          collections => Ok(views.html.language(user, language, collections))
        }
      }
  }

  def selectLanguage(langId: Int): EssentialAction = futureWithUser { user =>
    implicit request =>
      tableDefs.futureLanguageById(langId) flatMap {
        case None           => Future(onNoSuchLanguage(langId))
        case Some(language) => tableDefs.activateLanguageForUser(user, language) map {
          case true  => Redirect(routes.HomeController.allLanguages())
          case false => ???
        }
      }
  }

  def deselectLanguage(langId: Int): EssentialAction = futureWithUser { user =>
    implicit request =>
      tableDefs.futureLanguageById(langId) flatMap {
        case None           => Future(onNoSuchLanguage(langId))
        case Some(language) => tableDefs.deactivateLanguageForUser(user, language) map {
          case true  => Redirect(routes.HomeController.allLanguages())
          case false => ???
        }
      }
  }

  def collection(langId: Int, collId: Int): EssentialAction = futureWithUser { user =>
    implicit request =>
      tableDefs.futureLanguageById(langId) flatMap {
        case None           => Future(onNoSuchLanguage(langId))
        case Some(language) => tableDefs.futureCollectionById(language, collId) flatMap {
          case None             => Future(onNoSuchCollection(language, collId))
          case Some(collection) =>
            for {
              flashcardCount <- tableDefs.futureFlashcardCountForCollection(collection)
              toLearnCount <- tableDefs.futureFlashcardsToLearnCount(user, collection)
              toRepeatCount <- tableDefs.futureFlashcardsToRepeatCount(user, collection)
            } yield Ok(views.html.collection(user, language, collection, flashcardCount, toLearnCount, toRepeatCount))
        }
      }
  }


  def learn(langId: Int, collId: Int): EssentialAction = futureWithUser { user =>
    implicit request =>
      tableDefs.futureLanguageById(langId) flatMap {
        case None           => Future(onNoSuchLanguage(langId))
        case Some(language) => tableDefs.futureCollectionById(language, collId) flatMap {
          case None             => Future(onNoSuchCollection(language, collId))
          case Some(collection) => tableDefs.futureFlashcardsToLearnCount(user, collection) map {
            case 0 => ???
            case _ => Ok(views.html.learn(user, language, collection))
          }
        }
      }
  }


  def repeat(langId: Int, collId: Int): EssentialAction = futureWithUser { user =>
    implicit request =>
      ???
  }

}
