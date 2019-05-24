package model.persistence

import model.Consts._
import model._
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import slick.lifted.{ForeignKeyQuery, PrimaryKey, ProvenShape}

import scala.concurrent.ExecutionContext

trait CoursesCollectionsFlashcardsTableDefs
  extends HasDatabaseConfigProvider[JdbcProfile]
    with CoursesCollectionsFlashcardsTableQueries {

  protected val dbConfigProvider: DatabaseConfigProvider

  implicit val ec: ExecutionContext

  import profile.api._

  // TableQueries

  protected val coursesTQ: TableQuery[CoursesTable] = TableQuery[CoursesTable]

  protected val languagesTQ: TableQuery[LanguagesTable] = TableQuery[LanguagesTable]

  protected val collectionsTQ: TableQuery[CollectionsTable] = TableQuery[CollectionsTable]

  protected val flashcardsTQ: TableQuery[FlashcardsTable] = TableQuery[FlashcardsTable]

  protected val choiceAnswersTQ: TableQuery[ChoiceAnswersTable] = TableQuery[ChoiceAnswersTable]

  protected val blanksAnswersTQ: TableQuery[BlanksAnswerFragmentsTable] = TableQuery[BlanksAnswerFragmentsTable]

  // Column types

  protected implicit val cardTypeColumnType: BaseColumnType[CardType] =
    MappedColumnType.base[CardType, String](_.entryName, CardType.withNameInsensitive)

  protected implicit val correctnessColumnType: BaseColumnType[Correctness] =
    MappedColumnType.base[Correctness, String](_.entryName, Correctness.withNameInsensitive)

  // Table Defs

  class CoursesTable(tag: Tag) extends Table[Course](tag, "courses") {

    def id: Rep[Int] = column[Int](idName, O.PrimaryKey)

    def shortName: Rep[String] = column[String]("short_name")

    def title: Rep[String] = column[String](titleName)


    override def * : ProvenShape[Course] = (id, shortName, title) <> (Course.tupled, Course.unapply)

  }

  class LanguagesTable(tag: Tag) extends Table[Language](tag, "languages") {

    def id: Rep[Int] = column[Int](idName, O.PrimaryKey)

    def name: Rep[String] = column[String](nameName)


    override def * : ProvenShape[Language] = (id, name) <> (Language.tupled, Language.unapply)

  }

  class CollectionsTable(tag: Tag) extends Table[CollectionBasics](tag, "collections") {

    def id: Rep[Int] = column[Int](idName)

    def courseId: Rep[Int] = column[Int]("course_id")

    def frontLanguageId: Rep[Int] = column[Int]("front_language_id")

    def backLanguageId: Rep[Int] = column[Int]("back_language_id")

    def name: Rep[String] = column[String](nameName)


    def pk: PrimaryKey = primaryKey("coll_pk", (id, courseId))

    def courseFk: ForeignKeyQuery[CoursesTable, Course] = foreignKey("coll_course_fk", courseId, coursesTQ)(_.id)

    def frontLanguageFk: ForeignKeyQuery[LanguagesTable, Language] = foreignKey("coll_front_lang_fk", frontLanguageId, languagesTQ)(_.id)

    def backLanguageFk: ForeignKeyQuery[LanguagesTable, Language] = foreignKey("coll_back_lang_fk", backLanguageId, languagesTQ)(_.id)


    override def * : ProvenShape[CollectionBasics] = (id, courseId, frontLanguageId, backLanguageId, name) <> (CollectionBasics.tupled, CollectionBasics.unapply)

  }


  class FlashcardsTable(tag: Tag) extends Table[DBFlashcard](tag, "flashcards") {

    def id: Rep[Int] = column[Int](idName)

    def collId: Rep[Int] = column[Int]("coll_id")

    def courseId: Rep[Int] = column[Int]("course_id")

    def flashcardType: Rep[CardType] = column[CardType]("card_type")

    def question: Rep[String] = column[String](frontName)

    def questionHint: Rep[Option[String]] = column[Option[String]]("front_hint")

    def meaning: Rep[String] = column[String](backName)

    def meaningHint: Rep[Option[String]] = column[Option[String]]("back_hint")


    def pk: PrimaryKey = primaryKey("fc_pk", (id, collId, courseId))

    def collFk: ForeignKeyQuery[CollectionsTable, CollectionBasics] = foreignKey("fc_coll_fk", (collId, courseId), collectionsTQ)(coll => (coll.id, coll.courseId))


    override def * : ProvenShape[DBFlashcard] = (id, collId, courseId, flashcardType, question, questionHint, meaning, meaningHint) <> (DBFlashcard.tupled, DBFlashcard.unapply)

  }

  abstract class FlashcardComponentsTable[FC <: FlashcardComponent](tag: Tag, tableName: String) extends Table[FC](tag, tableName) {

    def id: Rep[Int] = column[Int](idName, O.AutoInc)

    def cardId: Rep[Int] = column[Int]("card_id")

    def collId: Rep[Int] = column[Int]("coll_id")

    def courseId: Rep[Int] = column[Int]("course_id")

    def answer: Rep[String] = column[String](answerName)


    def pk: PrimaryKey = primaryKey("ca_pk", (id, cardId, collId, courseId))

    def cardFk: ForeignKeyQuery[FlashcardsTable, DBFlashcard] = foreignKey("ca_card_fk", (cardId, collId, courseId), flashcardsTQ)(fc => (fc.id, fc.collId, fc.courseId))

  }

  class ChoiceAnswersTable(tag: Tag) extends FlashcardComponentsTable[ChoiceAnswer](tag, "choice_answers") {

    def correctness: Rep[Correctness] = column[Correctness](correctnessName)


    override def * : ProvenShape[ChoiceAnswer] = (id, cardId, collId, courseId, answer, correctness) <> (ChoiceAnswer.tupled, ChoiceAnswer.unapply)

  }

  class BlanksAnswerFragmentsTable(tag: Tag) extends FlashcardComponentsTable[BlanksAnswerFragment](tag, "blanks_answer_fragments") {

    def isAnswer: Rep[Boolean] = column[Boolean]("is_answer")


    override def * : ProvenShape[BlanksAnswerFragment] = (id, cardId, collId, courseId, answer, isAnswer) <> (BlanksAnswerFragment.tupled, BlanksAnswerFragment.unapply)

  }

}
