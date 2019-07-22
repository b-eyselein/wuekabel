package model.persistence

import model._

import scala.concurrent.Future

trait TableQueries {
  self: TableDefs =>

  import profile.api._

  // Helper methods

  private def flashcardToDoFilter(fctd: FlashcardToDoView[_ <: FlashcardToAnswerData], collection: Collection, user: User): Rep[Boolean] =
    fctd.collId === collection.id && fctd.courseId === collection.courseId && fctd.username === user.username

  def futureFlashcardToAnswerById(user: User, courseId: Int, collId: Int, cardId: Int, frontToBack: Boolean): Future[Option[FlashcardToAnswer]] = {
    val dbFlashcardByIdQuery = flashcardsTQ.filter {
      fc => fc.id === cardId && fc.collId === collId && fc.courseId === courseId
    }.result.headOption

    db.run(dbFlashcardByIdQuery).flatMap {
      case None                                                                   => Future.successful(None)
      case Some(DBFlashcard(_, _, _, cardType, front, frontHint, back, backHint)) =>

        for {
          choiceAnswersForDBFlashcard <- choiceAnswersForFlashcard(cardId, collId, courseId)
          blanksAnswersForDbFlashcard <- blanksAnswersForFlashcard(cardId, collId, courseId)
          maybeOldAnswer <- futureUserAnswerForFlashcard(user, cardId, collId, courseId, frontToBack)
        } yield {
          val frontToSend = if (frontToBack) front else back
          val frontHintToSend = if (frontToBack) frontHint else backHint

          Some(
            FlashcardToAnswer(
              cardId, collId, courseId, cardType,
              frontToSend,
              frontHintToSend,
              frontToBack,
              blanksAnswersForDbFlashcard,
              choiceAnswersForDBFlashcard,
              currentTries = maybeOldAnswer.map { oa => if (oa.isActive) oa.wrongTries else 0 }.getOrElse(0),
              currentBucket = maybeOldAnswer.map(_.bucket)
            )
          )
        }
    }
  }

  // FlashcardToLearn View

  def futureFlashcardsToLearnCount(user: User, collection: Collection): Future[Int] =
    db.run(flashcardsToLearnTQ.filter(flashcardToDoFilter(_, collection, user)).size.result)

  def futureMaybeNextFlashcardToLearn(user: User, collection: Collection): Future[Option[FlashcardToAnswer]] =
    db.run(flashcardsToLearnTQ.filter(flashcardToDoFilter(_, collection, user)).result.headOption).flatMap {
      case None                                                                  => Future.successful(None)
      case Some(FlashcardToAnswerData(cardId, collId, courseId, _, frontToBack)) =>
        futureFlashcardToAnswerById(user, courseId, collId, cardId, frontToBack)
    }

  def futureMaybeNextFlashcardToRepeat(user: User): Future[Option[FlashcardToAnswer]] =
    db.run(flashcardsToRepeatTQ.filter(_.username === user.username).result.headOption).flatMap {
      case None                                                                  => Future.successful(None)
      case Some(FlashcardToAnswerData(cardId, collId, courseId, _, frontToBack)) =>
        futureFlashcardToAnswerById(user, courseId, collId, cardId, frontToBack)
    }

  def futureFlashcardsToRepeatCount(user: User): Future[Int] = db.run(
    flashcardsToRepeatTQ.filter(_.username === user.username).size.result
  )

  def futureFlashcardsToRepeatCount(user: User, collection: Collection): Future[Int] = db.run(
    flashcardsToRepeatTQ.filter(flashcardToDoFilter(_, collection, user)).size.result
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
