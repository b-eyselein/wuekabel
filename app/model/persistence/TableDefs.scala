package model.persistence

import java.time.LocalDate

import javax.inject.Inject
import model.Consts._
import model._
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import slick.lifted.{ForeignKeyQuery, PrimaryKey, ProvenShape}

import scala.concurrent.ExecutionContext

class TableDefs @Inject()(override protected val dbConfigProvider: DatabaseConfigProvider)(implicit val ec: ExecutionContext)
  extends HasDatabaseConfigProvider[JdbcProfile]
    with UserInCourseTableDefs
    with CoursesCollectionsFlashcardsTableDefs
    with TableQueries {

  import profile.api._

  // Table queries

  protected val usersAnsweredFlashcardsTQ: TableQuery[UsersAnsweredFlashcardsTable] = TableQuery[UsersAnsweredFlashcardsTable]

  protected val flashcardsToLearnTQ: TableQuery[FlashcardsToLearnView] = TableQuery[FlashcardsToLearnView]

  protected val flashcardsToRepeatTQ: TableQuery[FlashcardsToRepeatView] = TableQuery[FlashcardsToRepeatView]

  // Table definitions

  class UsersAnsweredFlashcardsTable(tag: Tag) extends Table[UserAnsweredFlashcard](tag, "users_answered_flashcards") {

    def username: Rep[String] = column[String](usernameName)

    def cardId: Rep[Int] = column[Int]("card_id")

    def collId: Rep[Int] = column[Int]("coll_id")

    def courseId: Rep[Int] = column[Int]("course_id")

    def frontToBack: Rep[Boolean] = column[Boolean]("front_to_back", O.Default(true))


    def bucket: Rep[Int] = column[Int]("bucket")

    def dateAnswered: Rep[LocalDate] = column[LocalDate]("date_answered")

    def correct: Rep[Boolean] = column[Boolean](correctName)

    def tries: Rep[Int] = column[Int](triesName)


    def pk: PrimaryKey = primaryKey("uaf_pk", (username, cardId, collId))

    def userFk: ForeignKeyQuery[UsersTable, User] = foreignKey("uaf_user_fk", username, usersTQ)(_.username)

    def cardFk: ForeignKeyQuery[FlashcardsTable, DBFlashcard] = foreignKey("uaf_card_fk", (cardId, collId), flashcardsTQ)(fc => (fc.id, fc.collId))


    override def * : ProvenShape[UserAnsweredFlashcard] = (username, cardId, collId, courseId, bucket, dateAnswered, correct, tries, frontToBack) <> (UserAnsweredFlashcard.tupled, UserAnsweredFlashcard.unapply)

  }

  // Views

  abstract class FlashcardToDoView[T <: FlashcardToAnswerData](tag: Tag, tableName: String) extends Table[T](tag, tableName) {

    def cardId: Rep[Int] = column[Int]("card_id")

    def collId: Rep[Int] = column[Int]("coll_id")

    def courseId: Rep[Int] = column[Int]("course_id")

    def username: Rep[String] = column[String](usernameName)

    def frontToBack: Rep[Boolean] = column[Boolean]("front_to_back")

  }

  class FlashcardsToLearnView(tag: Tag) extends FlashcardToDoView[FlashcardToAnswerData](tag, "flashcards_to_learn") {

    override def * : ProvenShape[FlashcardToAnswerData] = (cardId, collId, courseId, username, frontToBack) <> (FlashcardToAnswerData.tupled, FlashcardToAnswerData.unapply)

  }

  class FlashcardsToRepeatView(tag: Tag) extends FlashcardToDoView[FlashcardToAnswerData](tag, "flashcards_to_repeat") {

    override def * : ProvenShape[FlashcardToAnswerData] = (cardId, collId, courseId, username, frontToBack) <> (FlashcardToAnswerData.tupled, FlashcardToAnswerData.unapply)

  }

}
