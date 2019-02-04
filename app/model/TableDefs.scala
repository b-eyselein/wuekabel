package model

import java.sql.{Date => SqlDate}
import java.time.LocalDate

import javax.inject.Inject
import model.Consts._
import model.persistence.{LanguageTableDefs, UserTableDefs}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import slick.lifted.{ForeignKeyQuery, PrimaryKey, ProvenShape}

import scala.concurrent.{ExecutionContext, Future}

class TableDefs @Inject()(override protected val dbConfigProvider: DatabaseConfigProvider)(implicit val ec: ExecutionContext)
  extends HasDatabaseConfigProvider[JdbcProfile] with UserTableDefs with LanguageTableDefs {

  import profile.api._

  // Table queries

  private val bucketsTQ: TableQuery[BucketsTable] = TableQuery[BucketsTable]

  private val userLearnsLanguageTQ: TableQuery[UserLearnsLanguageTable] = TableQuery[UserLearnsLanguageTable]

  private val usersAnsweredFlashcardsTQ: TableQuery[UsersAnsweredFlashcardsTable] = TableQuery[UsersAnsweredFlashcardsTable]

  private val flashcardsToLearnTQ: TableQuery[FlashcardsToLearnView] = TableQuery[FlashcardsToLearnView]

  private val flashcardsToRepeatTQ: TableQuery[FlashcardsToRepeatView] = TableQuery[FlashcardsToRepeatView]

  // Queries

  def futureLanguagesForUser(user: User): Future[Seq[Language]] = {
    val query = userLearnsLanguageTQ
      .join(languagesTQ).on(_.langId === _.id)
      .filter(_._1.username === user.username)
      .map(_._2)
      .result

    db.run(query)
  }

  // Queries - UserLearnsLanguage

  def futureUserLearnsLanguage(user: User, language: Language): Future[Boolean] = db.run(userLearnsLanguageTQ.filter {
    ull => ull.username === user.username && ull.langId === language.id
  }.result.headOption.map(_.isDefined))

  def futureLanguagesAndUserLearns(user: User): Future[Seq[(Language, Boolean)]] = db.run(languagesTQ.result) flatMap { languages =>
    Future.sequence {
      languages map { lang =>
        futureUserLearnsLanguage(user, lang) map {
          userLearnsLang => (lang, userLearnsLang)
        }
      }
    }
  }

  def activateLanguageForUser(user: User, language: Language): Future[Boolean] =
    db.run(userLearnsLanguageTQ += (user.username, language.id)).transform(_ == 1, identity)

  def deactivateLanguageForUser(user: User, language: Language): Future[Boolean] =
    db.run(userLearnsLanguageTQ.filter {
      ull => ull.username === user.username && ull.langId === language.id
    }.delete).transform(_ == 1, identity)

  // Queries - FlashcardToLearn View

  def futureFlashcardsToLearnCount(user: User, collection: Collection): Future[Int] = db.run(flashcardsToLearnTQ.filter {
    fctl => fctl.collId === collection.id && fctl.langId === collection.langId && fctl.username === user.username
  }.size.result)

  def futureMaybeIdentifierNextFlashcardToLearn(user: User, collection: Collection): Future[Option[FlashcardIdentifier]] =
    db.run(flashcardsToLearnTQ
      .filter { fctl => fctl.collId === collection.id && fctl.langId === collection.langId && fctl.username === user.username }
      .result
      .headOption
      .map {
        case None                              => None
        case Some((cardId, collId, langId, _)) => Some(FlashcardIdentifier(cardId, collId, langId))
      })

  def futureMaybeIdentifierNextFlashcardToRepeat(user: User, collection: Collection): Future[Option[FlashcardIdentifier]] =
    db.run(flashcardsToRepeatTQ
      .filter { fctr => fctr.collId === collection.id && fctr.langId === collection.langId && fctr.username === user.username }
      .result
      .headOption
      .map {
        case None                                    => None
        case Some((cardId, collId, langId, _, _, _)) => Some(FlashcardIdentifier(cardId, collId, langId))
      })

  def futureFlashcardsToRepeatCount(user: User, collection: Collection): Future[Int] =
    db.run(flashcardsToRepeatTQ.filter {
      fctr => fctr.collId === collection.id && fctr.langId === collection.langId && fctr.username === user.username
    }.size.result)

  def futureInsertOrUpdateUserAnswer(user: User, flashcard: Flashcard, correct: Boolean): Future[Boolean] = {
    val query: DBIO[Int] =
      sqlu"""
INSERT INTO users_answered_flashcards (username, card_id, coll_id, lang_id, bucket_id, date_answered, correct, tries)
VALUE (${user.username}, ${flashcard.id}, ${flashcard.collId}, ${flashcard.langId}, 1, NOW(), $correct, 1)
ON DUPLICATE KEY UPDATE date_answered = NOW(), correct = $correct,
                        bucket_id = IF($correct, bucket_id + 1, bucket_id),
                        tries = IF($correct, tries, tries + 1);"""

    db.run(query).transform(_ == 1, identity)
  }

  // Queries - UserAnsweredFlashcard

  def futureUserAnswerForFlashcard(user: User, flashcard: CompleteFlashcard): Future[Option[UserAnsweredFlashcard]] =
    db.run(usersAnsweredFlashcardsTQ.filter {
      uaf => uaf.username === user.username && uaf.cardId === flashcard.id && uaf.collId === flashcard.collId && uaf.langId === flashcard.langId
    }.result.headOption)

  // Column types

  private implicit val myDateColumnType: BaseColumnType[LocalDate] =
    MappedColumnType.base[LocalDate, SqlDate](SqlDate.valueOf, _.toLocalDate)

  // Table definitions


  class UserLearnsLanguageTable(tag: Tag) extends Table[(String, Int)](tag, "user_learns_language") {

    def username: Rep[String] = column[String](usernameName)

    def langId: Rep[Int] = column[Int]("lang_id")


    def pk: PrimaryKey = primaryKey("ull_pk", (username, langId))

    def userFk: ForeignKeyQuery[UsersTable, User] = foreignKey("ull_user_fk", username, usersTQ)(_.username)

    def langFk: ForeignKeyQuery[LanguagesTable, Language] = foreignKey("ull_lang_fk", langId, languagesTQ)(_.id)


    override def * : ProvenShape[(String, Int)] = (username, langId)

  }

  class BucketsTable(tag: Tag) extends Table[Bucket](tag, "buckets") {

    def id: Rep[Int] = column[Int](idName, O.PrimaryKey)

    def distanceDays: Rep[Int] = column[Int]("distance_days")


    override def * : ProvenShape[Bucket] = (id, distanceDays) <> (Bucket.tupled, Bucket.unapply)

  }


  class UsersAnsweredFlashcardsTable(tag: Tag) extends Table[UserAnsweredFlashcard](tag, "users_answered_flashcards") {

    def username: Rep[String] = column[String](usernameName)

    def cardId: Rep[Int] = column[Int]("card_id")

    def collId: Rep[Int] = column[Int]("coll_id")

    def langId: Rep[Int] = column[Int]("lang_id")

    def bucketId: Rep[Int] = column[Int]("bucket_id")

    def dateAnswered: Rep[LocalDate] = column[LocalDate]("date_answered")

    def correct: Rep[Boolean] = column[Boolean](correctName)

    def tries: Rep[Int] = column[Int](triesName)


    def pk: PrimaryKey = primaryKey("uaf_pk", (username, cardId, collId, langId))

    def userFk: ForeignKeyQuery[UsersTable, User] = foreignKey("uaf_user_fk", username, usersTQ)(_.username)

    def cardFk: ForeignKeyQuery[FlashcardsTable, Flashcard] = foreignKey("uaf_card_fk", (cardId, collId, langId), flashcardsTQ)(fc => (fc.id, fc.collId, fc.langId))


    override def * : ProvenShape[UserAnsweredFlashcard] = (username, cardId, collId, langId, bucketId, dateAnswered, correct, tries) <> (UserAnsweredFlashcard.tupled, UserAnsweredFlashcard.unapply)

  }

  // Views

  class FlashcardsToLearnView(tag: Tag) extends Table[(Int, Int, Int, String)](tag, "flashcards_to_learn") {

    def cardId: Rep[Int] = column[Int]("card_id")

    def collId: Rep[Int] = column[Int]("coll_id")

    def langId: Rep[Int] = column[Int]("lang_id")

    def username: Rep[String] = column[String](usernameName)


    def pk: PrimaryKey = primaryKey("fcs_to_learn_pk", (cardId, collId, langId, username))

    def userFk: ForeignKeyQuery[UsersTable, User] = foreignKey("fcs_to_learn_user_fk", username, usersTQ)(_.username)

    def cardFk: ForeignKeyQuery[FlashcardsTable, Flashcard] = foreignKey("fcs_to_learn_card_fk", (cardId, collId, langId), flashcardsTQ)(fc => (fc.id, fc.collId, fc.langId))


    override def * : ProvenShape[(Int, Int, Int, String)] = (cardId, collId, langId, username)

  }

  class FlashcardsToRepeatView(tag: Tag) extends Table[(Int, Int, Int, String, Boolean, Int)](tag, "flashcards_to_repeat") {

    def cardId: Rep[Int] = column[Int]("card_id")

    def collId: Rep[Int] = column[Int]("coll_id")

    def langId: Rep[Int] = column[Int]("lang_id")

    def username: Rep[String] = column[String](usernameName)

    def correct: Rep[Boolean] = column[Boolean](correctName)

    def tries: Rep[Int] = column[Int](triesName)


    override def * : ProvenShape[(Int, Int, Int, String, Boolean, Int)] = (cardId, collId, langId, username, correct, tries)

  }

}
