package model.persistence

import model._

import scala.concurrent.Future

trait TableQueries {
  self: TableDefs =>

  import profile.api._

  // Helper methods

  private def flashcardToDoFilter(fctd: FlashcardToDoView, collection: Collection, user: User): Rep[Boolean] =
    fctd.collId === collection.id && fctd.courseId === collection.courseId && fctd.username === user.username

  def futureFlashcardToAnswerById(user: User, courseId: Int, collId: Int, cardId: Int, frontToBack: Boolean): Future[FlashcardToAnswer] =
    futureFlashcardById(courseId, collId, cardId).flatMap {
      case None            => ???
      case Some(flashcard) =>

        for {
          maybeOldAnswer <- futureUserAnswerForFlashcard(user, cardId, collId, courseId, frontToBack)
        } yield {
          FlashcardToAnswer(
            flashcard, frontToBack,
            currentTries = maybeOldAnswer.map { oa => if (oa.isActive) oa.wrongTries else 0 }.getOrElse(0),
            currentBucket = maybeOldAnswer.map(_.bucket)
          )
        }
    }

  // FlashcardToLearn View

  private def futureSidesToLearnCount(user: User, collection: Collection, front: Boolean): Future[Int] = db.run(
    flashcardsToLearnTQ
      .filter { ftl => ftl.courseId === collection.courseId && ftl.collId === collection.id && ftl.username === user.username && ftl.frontToBack === front }
      .size
      .result
  )

  def futureFlashcardsToLearnCount(user: User, collection: Collection): Future[(Int, Int)] = for {
    frontsToLearnCount <- futureSidesToLearnCount(user, collection, front = true)
    backsToLearnCount <- futureSidesToLearnCount(user, collection, front = false)
  } yield (frontsToLearnCount, backsToLearnCount)

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
        .result.headOption
    )

}
