package controllers

import javax.inject.{Inject, Singleton}
import model._
import model.persistence.TableDefs
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class HomeController @Inject()(cc: ControllerComponents, protected val tableDefs: TableDefs)(implicit protected val ec: ExecutionContext)
  extends AbstractController(cc) with ControllerHelpers with play.api.i18n.I18nSupport {

  // Routes

  def index: EssentialAction = futureWithUser(adminRightsRequired = false) { user =>
    implicit request =>
      tableDefs.futureCoursesForUser(user) map { courses =>
        Ok(views.html.index(user, courses))
      }
  }

  def userPage: EssentialAction = futureWithUser(adminRightsRequired = false) { user =>
    implicit request =>
      tableDefs.futureCoursesForUser(user) map {
        courses => Ok(views.html.user(user, courses))
      }
  }

  def allLanguages: EssentialAction = futureWithUser(adminRightsRequired = false) { user =>
    implicit request =>
      tableDefs.futureLanguagesAndUserLearns(user) map { languagesAndUserLearns =>
        Ok(views.html.allLanguages(user, languagesAndUserLearns))
      }
  }

  def course(courseId: String): EssentialAction = futureWithUserAndCourse(adminRightsRequired = false, courseId) { (user, course) =>
    implicit request =>
      tableDefs.futureCollectionsForCourse(course) map {
        collectionsForCourse => Ok(views.html.course(user, course,collectionsForCourse))
      }
  }

  def language(langId: Int): EssentialAction =
    futureWithUserAndLanguage(adminRightsRequired = false, langId) { (user, language) =>
      implicit request =>
        //        tableDefs.futureCollectionsForLanguage(language) map {
        //          collections => Ok(views.html.language(user, language, collections))
        //        }
        ???
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

  def collection(collId: Int): EssentialAction =
    futureWithUserAndCollection(adminRightsRequired = false, collId) { (user, collection) =>
      implicit request =>
        for {
          flashcardCount <- tableDefs.futureFlashcardCountForCollection(collection)
          toLearnCount <- tableDefs.futureFlashcardsToLearnCount(user, collection)
          toRepeatCount <- tableDefs.futureFlashcardsToRepeatCount(user, collection)
        } yield Ok(views.html.collection(user, collection, flashcardCount, toLearnCount, toRepeatCount))
    }


  def startLearning(collId: Int, isRepeating: Boolean): EssentialAction =
    futureWithUserAndCollection(adminRightsRequired = false, collId) { (user, collection) =>
      implicit request =>
        val futureFlashcard = if (isRepeating)
          tableDefs.futureMaybeIdentifierNextFlashcardToRepeat(user, collection)
        else
          tableDefs.futureMaybeIdentifierNextFlashcardToLearn(user, collection)

        futureFlashcard map {
          case None             => Redirect(routes.HomeController.collection(collId))
          case Some(identifier) => Redirect(routes.HomeController.learn(identifier.collId, identifier.cardId, isRepeating))
        }
    }

  def learn(collId: Int, cardId: Int, isRepeating: Boolean): EssentialAction =
    futureWithUserAndCompleteFlashcard(adminRightsRequired = false, collId, cardId) { (user, _, flashcard) =>
      implicit request =>
        val futureMaybeOldAnswer: Future[Option[UserAnsweredFlashcard]] =
          tableDefs.futureUserAnswerForFlashcard(user, flashcard)


        futureMaybeOldAnswer map { maybeOldAnswer =>
          if (!isRepeating && maybeOldAnswer.isDefined) {
            // TODO: Something went wrong, take next flashcard?
            Redirect(routes.HomeController.startLearning(collId, isRepeating))
          } else {
            Ok(views.html.learn(user, flashcard, maybeOldAnswer, isRepeating))
          }
        }
    }

  def checkSolution(collId: Int, cardId: Int): EssentialAction =
    futureWithUserAndCompleteFlashcard(adminRightsRequired = false, collId, cardId) { (user, _, flashcard) =>
      implicit request =>
        request.body.asJson flatMap (json => JsonFormats.solutionFormat.reads(json).asOpt) match {
          case None           => Future(BadRequest("Could not read solution..."))
          case Some(solution) =>

            val futurePreviousTries: Future[Int] = tableDefs.futureUserAnswerForFlashcard(user, flashcard) map {
              case None                        => 0
              case Some(userAnsweredFlashcard) => userAnsweredFlashcard.tries
            }

            futurePreviousTries flatMap { previousTries =>

              if (previousTries >= 2) {
                ???
              } else {
                val correctionResult: CorrectionResult = Corrector.correct(flashcard, solution, previousTries)

                tableDefs.futureInsertOrUpdateUserAnswer(user, flashcard, correctionResult.correct) map {
                  _ => Ok(JsonFormats.correctionResultWrites.writes(correctionResult))
                }
              }
            }
        }
    }

}
