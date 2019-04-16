package model.persistence

import model.{BlanksAnswer, ChoiceAnswer, Collection, Course, Flashcard}

import scala.concurrent.Future

trait CoursesCollectionsFlashcardsTableQueries {
  self: CoursesCollectionsFlashcardsTableDefs =>

  import profile.api._

  // Numbers

  def futureNextCourseId: Future[Int] = db.run(coursesTQ.map(_.id).max.result).map(_.getOrElse(0))

  def futureNextCollectionIdInCourse(courseId: Int): Future[Int] = db.run(
    collectionsTQ.filter { coll => coll.courseId === courseId }.map(_.id).max.result
  ).map(_.getOrElse(0))

  // Reading

  def futureAllCourses: Future[Seq[Course]] = db.run(coursesTQ.result)

  def futureCourseById(id: Int): Future[Option[Course]] = db.run(coursesTQ.filter(_.id === id).result.headOption)

  def futureInsertCourse(course: Course): Future[Boolean] = db.run(coursesTQ += course).transform(_ == 1, identity)


  def futureAllCollectionsInCourse(courseId: Int): Future[Seq[Collection]] =
    db.run(collectionsTQ.filter(_.courseId === courseId).result)

  def futureCollectionById(courseId: Int, collId: Int): Future[Option[Collection]] =
    db.run(collectionsTQ.filter { coll => coll.id === collId && coll.courseId === courseId }.result.headOption)

  def futureInsertCollection(collection: Collection): Future[Boolean] = db.run(collectionsTQ += collection).transform(_ == 1, identity)


  private def blanksAnswersForDbFlashcard(dbfc: DBFlashcard): Future[Seq[BlanksAnswer]] = {
    val blanksAnswersForDbFlashcardQuery = blanksAnswersTQ.filter {
      ba => ba.cardId === dbfc.cardId && ba.collId === dbfc.collId
    }.result

    db.run(blanksAnswersForDbFlashcardQuery)
  }

  private def choiceAnswersForDbFlashcard(dbfc: DBFlashcard): Future[Seq[ChoiceAnswer]] = {

    val dbChoiceAnswersForDbFlashcardQuery = choiceAnswersTQ.filter {
      ca => ca.cardId === dbfc.cardId && ca.collId === dbfc.collId
    }.result

    db.run(dbChoiceAnswersForDbFlashcardQuery)
  }

  def futureFlashcardsForCollection(collection: Collection): Future[Seq[Flashcard]] = {
    val dbFlashcardsForFollQuery = flashcardsTQ.filter(_.collId === collection.id).result

    db.run(dbFlashcardsForFollQuery) flatMap { dbFlashcards: Seq[DBFlashcard] =>
      Future.sequence(dbFlashcards map { dbFlashcard =>

        for {
          choiceAnswers <- choiceAnswersForDbFlashcard(dbFlashcard)
          blanksAnswers <- blanksAnswersForDbFlashcard(dbFlashcard)
        } yield PersistenceModels.dbFlashcardToFlashcard(DBCompleteFlashcard(dbFlashcard, choiceAnswers, blanksAnswers))

      })
    }
  }

  def futureFlashcardById(collection: Collection, cardId: Int): Future[Option[Flashcard]] = {
    val dbFlashcardByIdQuery = flashcardsTQ.filter {
      fc => fc.id === cardId && fc.collId === collection.id
    }.result.headOption

    db.run(dbFlashcardByIdQuery) flatMap {
      case None                           => Future.successful(None)
      case Some(dbFlashcard: DBFlashcard) =>
        for {
          choiceAnswersForDBFlashcard <- choiceAnswersForDbFlashcard(dbFlashcard)
          blanksAnswersForDbFlashcard <- blanksAnswersForDbFlashcard(dbFlashcard)
        } yield Some(PersistenceModels.dbFlashcardToFlashcard(DBCompleteFlashcard(dbFlashcard, choiceAnswersForDBFlashcard, blanksAnswersForDbFlashcard)))
    }
  }

  def futureChoiceAnswersForFlashcard(flashcard: Flashcard): Future[Seq[ChoiceAnswer]] =
    db.run(choiceAnswersTQ.filter(ca => ca.cardId === flashcard.cardId && ca.collId === flashcard.collId).result)

  def futureFlashcardCountForCollection(collection: Collection): Future[Int] =
    db.run(flashcardsTQ.filter(fc => fc.collId === collection.id).size.result)

  // Saving

  def futureInsertCompleteFlashcard(completeFlashcard: Flashcard): Future[Boolean] = {
    val dbCompleteFlashcard = PersistenceModels.flashcardToDbFlashcard(completeFlashcard)

    futureInsertFlashcard(dbCompleteFlashcard.flashcard) flatMap { dbFlashcard =>

      val futureSavedChoiceAnswers = Future.sequence(dbCompleteFlashcard.choiceAnswers.map { dbChoiceAnswer =>
        futureInsertChoiceAnswer(dbChoiceAnswer.copy(cardId = dbFlashcard.cardId))
      })

      val futureSavedBlanksAnswers = Future.sequence(dbCompleteFlashcard.blanksAnswers.map { dbBlanksAnswer =>
        futureInsertBlanksAnswer(dbBlanksAnswer.copy(cardId = dbFlashcard.cardId))
      })

      futureSavedChoiceAnswers.map {
        savedAnswers => true
      }
    }
  }

  def futureInsertFlashcard(flashcard: DBFlashcard): Future[DBFlashcard] = {
    val query = flashcardsTQ returning flashcardsTQ.map(_.id) into ((fc, newId) => fc.copy(cardId = newId))

    db.run(query += flashcard)
  }

  private def futureInsertChoiceAnswer(choiceAnswer: ChoiceAnswer): Future[ChoiceAnswer] = {
    val query = choiceAnswersTQ returning choiceAnswersTQ.map(_.id) into ((ca, newId) => ca.copy(answerId = newId))

    db.run(query += choiceAnswer)
  }

  private def futureInsertBlanksAnswer(blanksAnswer: BlanksAnswer): Future[BlanksAnswer] = {
    val query = blanksAnswersTQ returning blanksAnswersTQ.map(_.id) into ((ca, newId) => ca.copy(answerId = newId))

    db.run(query += blanksAnswer)
  }

}
