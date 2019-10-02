package model.persistence

import model._

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
    collectionsTQ
      .filter { coll => coll.courseId === courseId }
      .map(_.id)
      .max
      .result
  )
    .map {
      case None            => 0
      case Some(currentId) => currentId + 1
    }

  // Reading

  def futureAllCourses: Future[Seq[Course]] = db.run(coursesTQ.result)

  def futureCourseById(id: Int): Future[Option[Course]] = db.run(coursesTQ.filter(_.id === id).result.headOption)

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
      .result
      .headOption
  ).map(_.map(PersistenceModels.collFromDbColl))

  def futureInsertCollection(collection: CollectionBasics): Future[Boolean] =
    db.run(collectionsTQ += collection).transform(_ == 1, identity)

  def futureFlashcardsForCollection(collection: Collection): Future[Seq[Flashcard]] = db.run(
    flashcardsTQ
      .filter { fc => fc.collId === collection.id && fc.courseId === collection.courseId }
      .result
  )

  def futureFlashcardById(courseId: Int, collId: Int, cardId: Int): Future[Option[Flashcard]] = db.run(
    flashcardsTQ
      .filter { fc => fc.id === cardId && fc.collId === collId && fc.courseId === courseId }
      .result
      .headOption
  )

  def futureFlashcardSidesCount(collection: Collection): Future[Int] = for {
    frontsCount <- db.run(
      flashcardsTQ
        .filter { fc => fc.collId === collection.id && fc.courseId === collection.courseId }
        .size
        .result
    )
    backsCount <- db.run(
      flashcardsTQ
        .filter { fc =>
          fc.collId === collection.id && fc.courseId === collection.courseId &&
            (fc.flashcardType.inSet(Seq(CardType.Word, CardType.Text)))
        }
        .size
        .result
    )
  } yield frontsCount + backsCount

  def futureFlashcardCountForCollection(collection: Collection): Future[Int] =
    db.run(flashcardsTQ.filter(fc => fc.collId === collection.id).size.result)

  // Saving

  def futureInsertCompleteFlashcard(flashcard: Flashcard): Future[Boolean] =
    db.run(flashcardsTQ.insertOrUpdate(flashcard)).transform(_ == 1, identity)

}
