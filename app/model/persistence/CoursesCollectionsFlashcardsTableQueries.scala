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
      .result
      .headOption
  ).map(_.map(PersistenceModels.collFromDbColl))

  def futureInsertCollection(collection: CollectionBasics): Future[Boolean] =
    db.run(collectionsTQ += collection).transform(_ == 1, identity)

  def futureFlashcardsForCollection(collection: Collection): Future[Seq[Flashcard]] = db.run(
    flashcardsTQ
      .filter { fc => fc.collId === collection.id && fc.courseId === collection.courseId }
      .result
  ).map(_.map(PersistenceModels.dbFlashcardToFlashcard))

  def futureFlashcardById(courseId: Int, collId: Int, cardId: Int): Future[Option[Flashcard]] = {
    val dbFlashcardByIdQuery = flashcardsTQ
      .filter { fc => fc.id === cardId && fc.collId === collId && fc.courseId === courseId }
      .result
      .headOption

    db.run(dbFlashcardByIdQuery).map {
      case None              => None
      case Some(dbFlashcard) => Some(PersistenceModels.dbFlashcardToFlashcard(dbFlashcard))
    }
  }


  def futureFlashcardCountForCollection(collection: Collection): Future[Int] =
    db.run(flashcardsTQ.filter(fc => fc.collId === collection.id).size.result)

  // Saving

  def futureInsertCompleteFlashcard(completeFlashcard: Flashcard): Future[Boolean] = {
    val flashcard = PersistenceModels.flashcardToDbFlashcard(completeFlashcard)

    db.run(flashcardsTQ insertOrUpdate flashcard).transform(_ == 1, identity)
  }

}
