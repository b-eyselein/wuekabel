package model.persistence

import model.{BlanksAnswerFragment, ChoiceAnswer, Collection, CollectionBasics, Course, Flashcard, Language}

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

  //  def futureCourseByShortName(shortName: String): Future[Option[Course]] =
  //    db.run(coursesTQ.filter(_.shortName === shortName).result.headOption)

  def futureInsertCourse(course: Course): Future[Boolean] = db.run(coursesTQ += course).transform(_ == 1, identity)


  def futureAllLanguages: Future[Seq[Language]] = db.run(languagesTQ.result)

  private lazy val completeCollectionTQ = collectionsTQ
    .join(languagesTQ).on(_.frontLanguageId === _.id)
    .join(languagesTQ).on(_._1.backLanguageId === _.id)
    .map { case ((dbColl, frontLang), backLang) => (dbColl, frontLang, backLang) }


  def futureAllCollectionsInCourse(courseId: Int): Future[Seq[Collection]] = db.run(
    completeCollectionTQ
      .filter { case (dbColl, _, _) => dbColl.courseId === courseId }
      .result
  ).map(_.map(PersistenceModels.collFromDbColl))

  def futureCollectionById(courseId: Int, collId: Int): Future[Option[Collection]] = db.run(
    completeCollectionTQ
      .filter { case (coll, _, _) => coll.id === collId && coll.courseId === courseId }
      .result.headOption
  ).map(_.map(PersistenceModels.collFromDbColl))

  def futureInsertCollection(collection: CollectionBasics): Future[Boolean] =
    db.run(collectionsTQ += collection).transform(_ == 1, identity)

  protected def blanksAnswersForFlashcard(cardId: Int, collId: Int, courseId: Int): Future[Seq[BlanksAnswerFragment]] = db.run(
    blanksAnswersTQ
      .filter { ba => ba.cardId === cardId && ba.collId === collId && ba.courseId === courseId }
      .result
  )

  protected def choiceAnswersForFlashcard(cardId: Int, collId: Int, courseId: Int): Future[Seq[ChoiceAnswer]] = db.run(
    choiceAnswersTQ
      .filter { ca => ca.cardId === cardId && ca.collId === collId && ca.courseId === courseId }
      .result
  )

  def futureFlashcardsForCollection(collection: Collection): Future[Seq[Flashcard]] = {
    val dbFlashcardsForFollQuery = flashcardsTQ
      .filter { fc => fc.collId === collection.id && fc.courseId === collection.courseId }
      .result

    db.run(dbFlashcardsForFollQuery) flatMap { dbFlashcards: Seq[DBFlashcard] =>
      Future.sequence(dbFlashcards.map {
        case DBFlashcard(cardId, collId, courseId, cardType, front, frontHint, back, backHint) =>

          for {
            choiceAnswers <- choiceAnswersForFlashcard(cardId, collId, courseId)
            blanksAnswers <- blanksAnswersForFlashcard(cardId, collId, courseId)
          } yield Flashcard(cardId, collId, courseId, cardType, front, frontHint, back, backHint, blanksAnswers, choiceAnswers)

      })
    }
  }

  def futureFlashcardById(courseId: Int, collId: Int, cardId: Int): Future[Option[Flashcard]] = {
    val dbFlashcardByIdQuery = flashcardsTQ.filter {
      fc => fc.id === cardId && fc.collId === collId && fc.courseId === courseId
    }.result.headOption

    db.run(dbFlashcardByIdQuery).flatMap {
      case None                                                                   => Future.successful(None)
      case Some(DBFlashcard(_, _, _, cardType, front, frontHint, back, backHint)) =>

        for {
          choiceAnswersForDBFlashcard <- choiceAnswersForFlashcard(cardId, collId, courseId)
          blanksAnswersForDbFlashcard <- blanksAnswersForFlashcard(cardId, collId, courseId)
        } yield Some(Flashcard(cardId, collId, courseId, cardType, front, frontHint, back, backHint, blanksAnswersForDbFlashcard, choiceAnswersForDBFlashcard))
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

  private def futureInsertBlanksAnswer(blanksAnswer: BlanksAnswerFragment): Future[Boolean] =
    db.run(blanksAnswersTQ insertOrUpdate blanksAnswer).transform(_ == 1, identity)

}
