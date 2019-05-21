package model.persistence

import model._

import scala.concurrent.Future

trait TableQueries {
  self: TableDefs =>

  import profile.api._

  // Helper methods

  private def flashcardToDoFilter(fctd: FlashcardToDoView[_ <: FlashcardToDoData], collection: Collection, user: User): Rep[Boolean] =
    fctd.collId === collection.id && fctd.courseId === collection.courseId && fctd.username === user.username

  // FlashcardToLearn View

  def futureFlashcardsToLearnCount(user: User, collection: Collection): Future[Int] =
    db.run(flashcardsToLearnTQ.filter(flashcardToDoFilter(_, collection, user)).size.result)

  def futureMaybeIdentifierNextFlashcardToLearn(user: User, collection: Collection): Future[Option[FlashcardIdentifier]] = db.run(
    flashcardsToLearnTQ
      .filter(flashcardToDoFilter(_, collection, user))
      .result.headOption.map(_.map(_.cardIdentifier))
  )

  def futureMaybeIdentifierNextFlashcardToRepeat(user: User, collection: Collection): Future[Option[FlashcardIdentifier]] = db.run(
    flashcardsToRepeatTQ
      .filter(flashcardToDoFilter(_, collection, user))
      .result.headOption.map(_.map(_.cardIdentifier))
  )

  def futureMaybeNextFlashcardToLearn(user: User, course: Course, collection: Collection): Future[Option[Flashcard]] =
    db.run(flashcardsToLearnTQ.filter(flashcardToDoFilter(_, collection, user)).result.headOption).flatMap {
      case None                             => Future.successful(None)
      case Some(fcId: FlashcardToLearnData) => futureFlashcardById(fcId.courseId, fcId.collId, fcId.cardId)
    }

  def futureMaybeNextFlashcardToRepeat(user: User): Future[Option[Flashcard]] =
    db.run(flashcardsToRepeatTQ.filter(_.username === user.username).result.headOption).flatMap {
      case None                              => Future.successful(None)
      case Some(fcId: FlashcardToRepeatData) => futureFlashcardById(fcId.courseId, fcId.collId, fcId.cardId)
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

  def futureUserAnswerForFlashcard(user: User, flashcard: Flashcard): Future[Option[UserAnsweredFlashcard]] =
    db.run(usersAnsweredFlashcardsTQ.filter {
      uaf => uaf.username === user.username && uaf.cardId === flashcard.cardId && uaf.collId === flashcard.collId
    }.result.headOption)

}
