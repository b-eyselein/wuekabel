package model.persistence

import model.{BlanksAnswer, ChoiceAnswer, Collection, Course, Flashcard}

import scala.concurrent.Future

trait CoursesCollectionsFlashcardsTableQueries {
  self: CoursesCollectionsFlashcardsTableDefs =>

  import profile.api._

  // Numbers

  def futureNextCourseId: Future[Int] = db.run(coursesTQ.map(_.id).max.result).map {
    case None            => 0
    case Some(currentId) => currentId + 1
  }

  def futureNextCollectionIdInCourse(courseId: Int): Future[Int] = db.run(
    collectionsTQ.filter { coll => coll.courseId === courseId }.map(_.id).max.result
  ).map {
    case None            => 0
    case Some(currentId) => currentId + 1
  }

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

  def futureInsertCompleteFlashcard(completeFlashcard: Flashcard): Future[Boolean] =
    PersistenceModels.flashcardToDbFlashcard(completeFlashcard) match {
      case DBCompleteFlashcard(flashcard, choiceAnswers, blanksAnswers) =>

        futureInsertFlashcard(flashcard) flatMap {
          case false => Future.successful(false)
          case true  =>

            for {
              futureSavedChoiceAnswers <- Future.sequence(choiceAnswers.map(futureInsertChoiceAnswer))
              futureSavedBlanksAnswers <- Future.sequence(blanksAnswers.map(futureInsertBlanksAnswer))
            } yield futureSavedChoiceAnswers.forall(identity) && futureSavedBlanksAnswers.forall(identity)
        }

    }


  def futureInsertFlashcard(flashcard: DBFlashcard): Future[Boolean] =
    db.run(flashcardsTQ insertOrUpdate flashcard).transform(_ == 1, identity)

  private def futureInsertChoiceAnswer(choiceAnswer: ChoiceAnswer): Future[Boolean] =
    db.run(choiceAnswersTQ insertOrUpdate choiceAnswer).transform(_ == 1, identity)

  private def futureInsertBlanksAnswer(blanksAnswer: BlanksAnswer): Future[Boolean] =
    db.run(blanksAnswersTQ insertOrUpdate blanksAnswer).transform(_ == 1, identity)

}
