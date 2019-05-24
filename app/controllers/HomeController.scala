package controllers

import javax.inject.{Inject, Singleton}
import model._
import model.persistence.TableDefs
import play.api.libs.json.JsString
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class HomeController @Inject()(cc: ControllerComponents, protected val tableDefs: TableDefs)(implicit protected val ec: ExecutionContext)
  extends AbstractController(cc) with ControllerHelpers with play.api.i18n.I18nSupport {

  //  private val logger = Logger(classOf[HomeController])

  override protected val adminRightsRequired: Boolean = false

  // Routes

  def index: EssentialAction = futureWithUser { user =>
    implicit request =>
      val pwSet = request.flash.get("no_pw_set").isEmpty

      for {
        courses <- tableDefs.futureCoursesForUser(user.username)
        repeatCount <- tableDefs.futureFlashcardsToRepeatCount(user)
      } yield Ok(views.html.index(user, courses, repeatCount, pwSet))
  }

  def registerForCoursesForm: EssentialAction = futureWithUser { user =>
    implicit request =>
      tableDefs.futureAllCoursesWithRegisterState(user.username).map {
        allCoursesAndRegisterState => Ok(views.html.forms.registerForCoursesForm(user, allCoursesAndRegisterState))
      }
  }

  def registerForCourse(courseId: Int): EssentialAction = futureWithUser { user =>
    implicit request =>
      tableDefs.futureRegisterUserForCourse(user.username, courseId).map {
        _ => Redirect(controllers.routes.HomeController.registerForCoursesForm())
      }
  }

  def unregisterForCourse(courseId: Int): EssentialAction = futureWithUser { user =>
    implicit request =>
      tableDefs.futureUnregisterUserFromCourse(user.username, courseId).map {
        _ => Redirect(controllers.routes.HomeController.registerForCoursesForm())
      }
  }

  def userPage: EssentialAction = futureWithUser { user =>
    implicit request =>
      tableDefs.futureCoursesForUser(user.username) map {
        courses => Ok(views.html.user(user, courses))
      }
  }

  def course(courseId: Int): EssentialAction = futureWithUserAndCourse(courseId) { (user, course) =>
    implicit request =>
      tableDefs.futureAllCollectionsInCourse(course.id) map {
        collectionsForCourse => Ok(views.html.course(user, course, collectionsForCourse))
      }
  }

  def collection(courseId: Int, collId: Int): EssentialAction = futureWithUserAndCollection(courseId, collId) { (user, course, collection) =>
    implicit request =>
      for {
        flashcardCount <- tableDefs.futureFlashcardCountForCollection(collection)
        toLearnCount <- tableDefs.futureFlashcardsToLearnCount(user, collection)
        toRepeatCount <- tableDefs.futureFlashcardsToRepeatCount(user, collection)
      } yield Ok(views.html.collection(user, courseId, collection, flashcardCount, toLearnCount, toRepeatCount))
  }

  def learn(courseId: Int, collId: Int, frontToBack: Boolean = true): EssentialAction = futureWithUserAndCollection(courseId, collId) { (user, course, collection) =>
    implicit request =>
      tableDefs.futureFlashcardsToLearnCount(user, collection).map {
        cardsToLearnCount => Ok(views.html.learn(user, Math.min(cardsToLearnCount, 10), Some(course, collection)))
      }
  }

  def nextFlashcardToLearn(courseId: Int, collId: Int): EssentialAction = futureWithUserAndCollection(courseId, collId) { (user, _, collection) =>
    implicit request =>
      tableDefs.futureMaybeNextFlashcardToLearn(user, collection).map {
        case None     => NotFound("No Flashcard to learn found")
        case Some(fc) => Ok(JsonFormats.flashcardToAnswerFormat.writes(fc))
      }
  }

  def repeat: EssentialAction = futureWithUser { user =>
    implicit request =>
      tableDefs.futureFlashcardsToRepeatCount(user).map {
        cardsToRepeatCount => Ok(views.html.learn(user, Math.min(cardsToRepeatCount, 10), None))
      }
  }

  def nextFlashcardToRepeat: EssentialAction = futureWithUser { user =>
    implicit request =>
      tableDefs.futureMaybeNextFlashcardToRepeat(user).map {
        case None     => NotFound("There has been an error?")
        case Some(fc) => Ok(JsonFormats.flashcardToAnswerFormat.writes(fc))
      }
  }

  def checkSolution: EssentialAction = futureWithUser { user =>
    implicit request =>
      request.body.asJson.flatMap(json => JsonFormats.solutionFormat.reads(json).asOpt) match {
        case None           => Future.successful(BadRequest(JsString("Could not read solution...")))
        case Some(solution) =>

          tableDefs.futureFlashcardById(solution.courseId, solution.collId, solution.cardId).flatMap {
            case None            => ???
            case Some(flashcard) =>

              tableDefs.futureUserAnswerForFlashcard(user, flashcard.cardId, flashcard.collId, flashcard.courseId, solution.frontToBack).flatMap {
                maybePreviousAnswer: Option[UserAnsweredFlashcard] =>

                  val (corrResult, newAnswer) = Corrector.completeCorrect(user, solution, flashcard, maybePreviousAnswer)

                  tableDefs.futureInsertOrUpdateUserAnswer(newAnswer).map { _ =>
                    Ok(JsonFormats.completeCorrectionResultFormat.writes(corrResult))
                  }

              }
          }
      }
  }


}
