package model.persistence

import model.JsonFormats.{blanksAnswerFragmentFormat, choiceAnswerFormat}
import model._
import play.api.libs.json.{Format, JsError, JsSuccess, Json}

import scala.concurrent.Future

trait TableQueries {
  self: TableDefs =>

  import profile.api._

  // Helper methods

  private def flashcardToDoFilter(fctd: FlashcardToDoView, collection: Collection, user: User): Rep[Boolean] =
    fctd.collId === collection.id && fctd.courseId === collection.courseId && fctd.username === user.username

  def futureFlashcardToAnswerById(user: User, courseId: Int, collId: Int, cardId: Int, frontToBack: Boolean): Future[FlashcardToAnswer] = {

    // FIXME: join tables, use method in PersistenceModels!
    val dbFlashcardByIdQuery = flashcardsTQ.filter {
      fc => fc.id === cardId && fc.collId === collId && fc.courseId === courseId
    }.result.headOption

    db.run(dbFlashcardByIdQuery).flatMap {
      case None                                                                                                                       => ???
      case Some(DBFlashcard(_, _, _, cardType, front, frontHint, back, backHint, blanksAnswerFragmentsJsValue, choiceAnswersJsValue)) =>

        implicit val caf: Format[ChoiceAnswer] = choiceAnswerFormat
        val choiceAnswers: Seq[ChoiceAnswer] = Json.fromJson[Seq[ChoiceAnswer]](choiceAnswersJsValue) match {
          case JsSuccess(cas, _) => cas
          case JsError(errors)   => ???
        }

        implicit val baff: Format[BlanksAnswerFragment] = blanksAnswerFragmentFormat
        val blanksAnswerFragments: Seq[BlanksAnswerFragment] = Json.fromJson[Seq[BlanksAnswerFragment]](blanksAnswerFragmentsJsValue) match {
          case JsSuccess(bafs, _) => bafs
          case JsError(errors)    => ???
        }

        for {
          maybeOldAnswer <- futureUserAnswerForFlashcard(user, cardId, collId, courseId, frontToBack)
        } yield {
          val frontToSend = if (frontToBack) front else back
          val frontHintToSend = if (frontToBack) frontHint else backHint

          FlashcardToAnswer(
            cardId, collId, courseId, cardType,
            frontToSend,
            frontHintToSend,
            frontToBack,
            blanksAnswerFragments,
            choiceAnswers,
            currentTries = maybeOldAnswer.map { oa => if (oa.isActive) oa.wrongTries else 0 }.getOrElse(0),
            currentBucket = maybeOldAnswer.map(_.bucket)
          )
        }
    }
  }

  // FlashcardToLearn View

  def futureFlashcardsToLearnCount(user: User, collection: Collection): Future[Int] =
    db.run(flashcardsToLearnTQ.filter(flashcardToDoFilter(_, collection, user)).size.result)

  def futureMaybeNextFlashcardToLearn(user: User, collection: Collection, count: Int = 10): Future[Seq[FlashcardToAnswer]] = {
    val query = flashcardsToLearnTQ
      .filter(flashcardToDoFilter(_, collection, user))
      .take(count)
      .result

    db.run(query).flatMap { answerData: Seq[FlashcardToAnswerData] =>
      Future.sequence(answerData.map {
        case FlashcardToAnswerData(cardId, collId, courseId, _, frontToBack) =>
          futureFlashcardToAnswerById(user, courseId, collId, cardId, frontToBack)
      })
    }
  }

  def futureMaybeNextFlashcardToRepeat(user: User, count: Int = 10): Future[Seq[FlashcardToAnswer]] = {
    val query = flashcardsToRepeatTQ
      .filter(_.username === user.username)
      .take(count)
      .result

    db.run(query).flatMap { answerData: Seq[FlashcardToAnswerData] =>
      Future.sequence(answerData.map {
        case FlashcardToAnswerData(cardId, collId, courseId, _, frontToBack) =>
          futureFlashcardToAnswerById(user, courseId, collId, cardId, frontToBack)
      })
    }
  }

  def futureFlashcardsToRepeatCount(user: User): Future[Int] = db.run(
    flashcardsToRepeatTQ.filter(_.username === user.username).size.result
  )

  def futureInsertOrUpdateUserAnswer(newAnswer: UserAnsweredFlashcard): Future[Boolean] =
    db.run(usersAnsweredFlashcardsTQ insertOrUpdate newAnswer).transform(_ == 1, identity)

  // Queries - UserAnsweredFlashcard

  def futureUserAnswerForFlashcard(user: User, cardId: Int, collId: Int, courseId: Int, frontToBack: Boolean): Future[Option[UserAnsweredFlashcard]] =
    db.run(
      usersAnsweredFlashcardsTQ
        .filter {
          uaf => uaf.username === user.username && uaf.cardId === cardId && uaf.collId === collId && uaf.courseId === courseId && uaf.frontToBack === frontToBack
        }
        .result.headOption)

}
