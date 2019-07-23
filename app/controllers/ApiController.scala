package controllers

import javax.inject.{Inject, Singleton}
import model.persistence.TableDefs
import model.{Corrector, FlashcardToAnswer, JsonFormats, UserAnsweredFlashcard}
import play.api.Logger
import play.api.libs.json.{Format, JsString, Json}
import play.api.mvc.{AbstractController, ControllerComponents, EssentialAction}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ApiController @Inject()(cc: ControllerComponents, protected val tableDefs: TableDefs)(implicit protected val ec: ExecutionContext)
  extends AbstractController(cc) with NewControllerHelpers {

  private val logger = Logger(classOf[ApiController])

  override protected val adminRightsRequired: Boolean = false

  // Routes

  def nextFlashcardsToRepeat(count: Int): EssentialAction = futureWithUser { user =>
    implicit request =>

      implicit val fctaf: Format[FlashcardToAnswer] = JsonFormats.flashcardToAnswerFormat

      tableDefs.futureMaybeNextFlashcardToRepeat(user, count).map { fc => Ok(Json.toJson(fc)) }
  }

  def nextFlashcardsToLearn(courseId: Int, collId: Int, count: Int): EssentialAction = futureWithUserAndCollection(courseId, collId) { (user, _, collection) =>
    implicit request =>

      implicit val fctaf: Format[FlashcardToAnswer] = JsonFormats.flashcardToAnswerFormat

      tableDefs.futureMaybeNextFlashcardToLearn(user, collection, count).map { fc => Ok(Json.toJson(fc)) }
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
