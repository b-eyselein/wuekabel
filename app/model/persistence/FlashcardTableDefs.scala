package model.persistence

import model.Consts._
import model._
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import slick.lifted.{ForeignKeyQuery, PrimaryKey, ProvenShape}

import scala.concurrent.{ExecutionContext, Future}

trait FlashcardTableDefs extends HasDatabaseConfigProvider[JdbcProfile] with CollectionTableDefs {

  protected val dbConfigProvider: DatabaseConfigProvider

  implicit val ec: ExecutionContext

  import profile.api._

  // TableQueries

  protected val flashcardsTQ: TableQuery[FlashcardsTable] = TableQuery[FlashcardsTable]

  protected val choiceAnswersTQ: TableQuery[ChoiceAnswersTable] = TableQuery[ChoiceAnswersTable]

  protected val blanksAnswersTQ: TableQuery[BlanksAnswersTable] = TableQuery[BlanksAnswersTable]

  // Queries


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

  def futureInsertChoiceAnswer(choiceAnswer: ChoiceAnswer): Future[ChoiceAnswer] = {
    val query = choiceAnswersTQ returning choiceAnswersTQ.map(_.id) into ((ca, newId) => ca.copy(answerId = newId))

    db.run(query += choiceAnswer)
  }

  def futureInsertBlanksAnswer(blanksAnswer: BlanksAnswer): Future[BlanksAnswer] = {
    val query = blanksAnswersTQ returning blanksAnswersTQ.map(_.id) into ((ca, newId) => ca.copy(answerId = newId))

    db.run(query += blanksAnswer)
  }

  // Column types

  protected implicit val cardTypeColumnType: BaseColumnType[CardType] =
    MappedColumnType.base[CardType, String](_.entryName, CardType.withNameInsensitive)

  protected implicit val correctnessColumnType: BaseColumnType[Correctness] =
    MappedColumnType.base[Correctness, String](_.entryName, Correctness.withNameInsensitive)

  // Table Defs

  class FlashcardsTable(tag: Tag) extends Table[DBFlashcard](tag, "flashcards") {

    def id: Rep[Int] = column[Int](idName, O.AutoInc)

    def collId: Rep[Int] = column[Int]("coll_id")

    def flashcardType: Rep[CardType] = column[CardType]("flash_card_type")

    def question: Rep[String] = column[String](questionName)

    def meaning: Rep[Option[String]] = column[Option[String]](meaningName)


    def pk: PrimaryKey = primaryKey("fc_pk", (id, collId))

    def collFk: ForeignKeyQuery[CollectionsTable, Collection] = foreignKey("fc_coll_fk", collId, collectionsTQ)(_.id)


    override def * : ProvenShape[DBFlashcard] = (id, collId, flashcardType, question, meaning) <> (DBFlashcard.tupled, DBFlashcard.unapply)

  }

  abstract class CardAnswersTable[CA <: CardAnswer](tag: Tag, tableName: String) extends Table[CA](tag, tableName) {

    def id: Rep[Int] = column[Int](idName, O.AutoInc)

    def cardId: Rep[Int] = column[Int]("card_id")

    def collId: Rep[Int] = column[Int]("coll_id")

    def answer: Rep[String] = column[String](answerName)


    def pk: PrimaryKey = primaryKey("ca_pk", (id, cardId, collId))

    def cardFk: ForeignKeyQuery[FlashcardsTable, DBFlashcard] = foreignKey("ca_card_fk", (cardId, collId), flashcardsTQ)(fc => (fc.id, fc.collId))

  }

  class ChoiceAnswersTable(tag: Tag) extends CardAnswersTable[ChoiceAnswer](tag, "choice_answers") {

    def correctness: Rep[Correctness] = column[Correctness](correctnessName)


    override def * : ProvenShape[ChoiceAnswer] = (id, cardId, collId, answer, correctness) <> (ChoiceAnswer.tupled, ChoiceAnswer.unapply)

  }

  class BlanksAnswersTable(tag: Tag) extends CardAnswersTable[BlanksAnswer](tag, "blanks_answers") {

    override def * : ProvenShape[BlanksAnswer] = (id, cardId, collId, answer) <> (BlanksAnswer.tupled, BlanksAnswer.unapply)

  }

}
