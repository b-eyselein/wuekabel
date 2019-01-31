package model.persistence

import model.Consts._
import model._
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import slick.lifted.{ForeignKeyQuery, PrimaryKey, ProvenShape}

import scala.concurrent.{ExecutionContext, Future}

trait LanguageTableDefs extends HasDatabaseConfigProvider[JdbcProfile] {

  protected val dbConfigProvider: DatabaseConfigProvider

  implicit val ec: ExecutionContext

  import profile.api._

  // TableQueries

  protected val languagesTQ: TableQuery[LanguagesTable] = TableQuery[LanguagesTable]

  protected val collectionsTQ: TableQuery[CollectionsTable] = TableQuery[CollectionsTable]

  protected val flashcardsTQ: TableQuery[FlashcardsTable] = TableQuery[FlashcardsTable]

  protected val choiceAnswersTQ: TableQuery[ChoiceAnswersTable] = TableQuery[ChoiceAnswersTable]

  // Queries - Language

  def futureLanguageById(langId: Int): Future[Option[Language]] = db.run(languagesTQ.filter(_.id === langId).result.headOption)

  def futureAllLanguages: Future[Seq[Language]] = db.run(languagesTQ.result)

  def futureCollectionsForLanguage(language: Language): Future[Seq[Collection]] =
    db.run(collectionsTQ.filter(_.langId === language.id).result)

  def futureInsertLanguage(language: Language): Future[Int] = {
    val query = languagesTQ returning languagesTQ.map(_.id) into ((lang, newId) => lang.copy(id = newId))

    db.run(query += language).map(_.id)
  }

  // Queries - Collection

  def futureCollectionById(language: Language, collId: Int): Future[Option[Collection]] =
    db.run(collectionsTQ.filter(coll => coll.langId === language.id && coll.id === collId).result.headOption)

  def futureInsertCollection(collection: Collection): Future[Int] = {
    val query = collectionsTQ returning collectionsTQ.map(_.id) into ((coll, newId) => coll.copy(id = newId))

    db.run(query += collection).map(_.id)
  }

  // Queries - Flashcard

  def futureFlashcardsForCollection(collection: Collection): Future[Seq[Flashcard]] =
    db.run(flashcardsTQ.filter(fc => fc.collId === collection.id && fc.langId === collection.langId).result)

  def futureFlashcardById(collection: Collection, cardId: Int): Future[Option[Flashcard]] =
    db.run(flashcardsTQ.filter {
      fc => fc.id === cardId && fc.collId === collection.id && fc.langId === collection.langId
    }.result.headOption)

  def futureChoiceAnswersForFlashcard(flashcard: Flashcard): Future[Seq[ChoiceAnswer]] =
    db.run(choiceAnswersTQ.filter {
      ca => ca.cardId === flashcard.id && ca.collId === flashcard.collId && ca.langId === flashcard.langId
    }.result)

  def futureFlashcardCountForCollection(collection: Collection): Future[Int] =
    db.run(flashcardsTQ.filter(fc => fc.collId === collection.id && fc.langId === collection.langId).size.result)


  def futureInsertCompleteFlashcard(completeFlashcard: CompleteFlashcard): Future[CompleteFlashcard] =
    futureInsertFlashcard(completeFlashcard.flashcard) flatMap { flashcard =>

      val futureSavedAnswers = Future.sequence(completeFlashcard.choiceAnswers.map { choiceAnswer =>
        futureInsertChoiceAnswer(choiceAnswer.copy(cardId = flashcard.id))
      })

      futureSavedAnswers.map {
        savedAnswers => CompleteFlashcard(flashcard, savedAnswers)
      }
    }

  def futureInsertFlashcard(flashcard: Flashcard): Future[Flashcard] = {
    val query = flashcardsTQ returning flashcardsTQ.map(_.id) into ((fc, newId) => fc.copy(id = newId))

    db.run(query += flashcard)
  }

  def futureInsertChoiceAnswer(choiceAnswer: ChoiceAnswer): Future[ChoiceAnswer] = {
    val query = choiceAnswersTQ returning choiceAnswersTQ.map(_.id) into ((ca, newId) => ca.copy(id = newId))

    db.run(query += choiceAnswer)
  }

  // Column types

  protected implicit val cardTypeColumnType: BaseColumnType[CardType] =
    MappedColumnType.base[CardType, String](_.entryName, CardType.withNameInsensitive)

  protected implicit val correctnessColumnType: BaseColumnType[Correctness] =
    MappedColumnType.base[Correctness, String](_.entryName, Correctness.withNameInsensitive)

  // Table definitions

  class LanguagesTable(tag: Tag) extends Table[Language](tag, "languages") {

    def id: Rep[Int] = column[Int](idName, O.PrimaryKey, O.AutoInc)

    def name: Rep[String] = column[String](nameName)


    override def * : ProvenShape[Language] = (id, name) <> (Language.tupled, Language.unapply)

  }

  class CollectionsTable(tag: Tag) extends Table[Collection](tag, "collections") {

    def id: Rep[Int] = column[Int](idName, O.AutoInc)

    def langId: Rep[Int] = column[Int]("lang_id")

    def name: Rep[String] = column[String](nameName)


    def pk: PrimaryKey = primaryKey("coll_pk", (id, langId))

    def langFk: ForeignKeyQuery[LanguagesTable, Language] = foreignKey("coll_language_fk", langId, languagesTQ)(_.id)


    override def * : ProvenShape[Collection] = (id, langId, name) <> (Collection.tupled, Collection.unapply)

  }

  class FlashcardsTable(tag: Tag) extends Table[Flashcard](tag, "flashcards") {

    def id: Rep[Int] = column[Int](idName, O.AutoInc)

    def collId: Rep[Int] = column[Int]("coll_id")

    def langId: Rep[Int] = column[Int]("lang_id")

    def flashcardType: Rep[CardType] = column[CardType]("flash_card_type")

    def question: Rep[String] = column[String](questionName)

    def meaning: Rep[Option[String]] = column[Option[String]](meaningName)


    def pk: PrimaryKey = primaryKey("fc_pk", (id, collId, langId))

    def collFk: ForeignKeyQuery[CollectionsTable, Collection] = foreignKey("fc_coll_fk", (collId, langId), collectionsTQ)(c => (c.id, c.langId))


    override def * : ProvenShape[Flashcard] = (id, collId, langId, flashcardType, question, meaning) <> (Flashcard.tupled, Flashcard.unapply)

  }

  class ChoiceAnswersTable(tag: Tag) extends Table[ChoiceAnswer](tag, "choice_answers") {

    def id: Rep[Int] = column[Int](idName, O.AutoInc)

    def cardId: Rep[Int] = column[Int]("card_id")

    def collId: Rep[Int] = column[Int]("coll_id")

    def langId: Rep[Int] = column[Int]("lang_id")

    def answer: Rep[String] = column[String](answerName)

    def correctness: Rep[Correctness] = column[Correctness](correctnessName)


    def pk: PrimaryKey = primaryKey("ca_pk", (id, cardId, collId, langId))

    def cardFk: ForeignKeyQuery[FlashcardsTable, Flashcard] = foreignKey("ca_card_fk", (cardId, collId, langId), flashcardsTQ)(fc => (fc.id, fc.collId, fc.langId))


    override def * : ProvenShape[ChoiceAnswer] = (id, cardId, collId, langId, answer, correctness) <> (ChoiceAnswer.tupled, ChoiceAnswer.unapply)

  }

}
