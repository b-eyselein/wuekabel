package model


import java.sql.Date
import java.time.LocalDate

import javax.inject.Inject
import model.Consts._
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import slick.lifted.{ForeignKeyQuery, PrimaryKey, ProvenShape}

import scala.concurrent.{ExecutionContext, Future}

class TableDefs @Inject()(override protected val dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext)
  extends HasDatabaseConfigProvider[JdbcProfile] {

  import profile.api._

  // Table queries

  private val bucketsTQ: TableQuery[BucketsTable] = TableQuery[BucketsTable]

  private val usersTQ        : TableQuery[UsersTable]         = TableQuery[UsersTable]
  private val userPasswordsTQ: TableQuery[UserPasswordsTable] = TableQuery[UserPasswordsTable]

  private val languagesTQ    : TableQuery[LanguagesTable]     = TableQuery[LanguagesTable]
  private val collectionsTQ  : TableQuery[CollectionsTable]   = TableQuery[CollectionsTable]
  private val flashcardsTQ   : TableQuery[FlashcardsTable]    = TableQuery[FlashcardsTable]
  private val choiceAnswersTQ: TableQuery[ChoiceAnswersTable] = TableQuery[ChoiceAnswersTable]

  private val userLearnsLanguageTQ: TableQuery[UserLearnsLanguageTable] = TableQuery[UserLearnsLanguageTable]

  private val usersAnsweredFlashcardsTQ: TableQuery[UsersAnsweredFlashcardsTable] = TableQuery[UsersAnsweredFlashcardsTable]

  private val flashcardsToLearnTQ : TableQuery[FlashcardsToLearnView]  = TableQuery[FlashcardsToLearnView]
  private val flashcardsToRepeatTQ: TableQuery[FlashcardsToRepeatView] = TableQuery[FlashcardsToRepeatView]

  // Queries

  def futureUserByUserName(username: String): Future[Option[User]] = db.run(usersTQ.filter(_.username === username).result.headOption)

  def futurePwHashForUser(username: String): Future[Option[UserPassword]] = db.run(userPasswordsTQ.filter(_.username === username).result.headOption)

  def futureSaveUser(user: User): Future[Boolean] = db.run(usersTQ += user).transform(_ == 1, identity)

  def savePwHash(userPassword: UserPassword): Future[Boolean] = db.run(userPasswordsTQ += userPassword).transform(_ == 1, identity)

  def futureLanguageById(langId: Int): Future[Option[Language]] = db.run(languagesTQ.filter(_.id === langId).result.headOption)

  def futureLanguagesForUser(user: User): Future[Seq[Language]] = {
    val query = userLearnsLanguageTQ
      .join(languagesTQ).on(_.langId === _.id)
      .filter(_._1.username === user.username)
      .map(_._2)
      .result

    db.run(query)
  }

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

  def futureCollectionsForLanguage(language: Language): Future[Seq[Collection]] =
    db.run(collectionsTQ.filter(_.langId === language.id).result)

  def futureCollectionById(language: Language, collId: Int): Future[Option[Collection]] =
    db.run(collectionsTQ.filter(coll => coll.langId === language.id && coll.id === collId).result.headOption)

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

  def futureFlashcardsToLearnCount(user: User, collection: Collection): Future[Int] = db.run(flashcardsToLearnTQ.filter {
    fctl => fctl.collId === collection.id && fctl.langId === collection.langId && fctl.username === user.username
  }.size.result)

  def futureMaybeIdentifierNextFlashcardToLearn(user: User, collection: Collection): Future[Option[FlashcardIdentifier]] =
    db.run(
      flashcardsToLearnTQ
        .filter { fctl => fctl.collId === collection.id && fctl.langId === collection.langId && fctl.username === user.username }
        .result
        .headOption
        .map {
          case None                              => None
          case Some((cardId, collId, langId, _)) => Some(FlashcardIdentifier(cardId, collId, langId))
        }
    )

  def futureFlashcardsToLearn(user: User, collection: Collection): Future[Seq[Flashcard]] = {
    db.run(flashcardsToLearnTQ
      .filter { fctl => fctl.collId === collection.id && fctl.langId === collection.langId && fctl.username === user.username }
      .join(flashcardsTQ)
      .on { case (fctl, fc) => fctl.cardId === fc.id && fctl.collId === fc.collId && fctl.langId === fc.langId }
      .map { case (fctl, fc) => fc }
      .result)
  }

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

  // Column types

  private implicit val cardTypeColumnType: BaseColumnType[CardType] =
    MappedColumnType.base[CardType, String](_.entryName, CardType.withNameInsensitive)

  private implicit val correctnessColumnType: BaseColumnType[Correctness] =
    MappedColumnType.base[Correctness, String](_.entryName, Correctness.withNameInsensitive)

  private implicit val myDateColumnType: BaseColumnType[LocalDate] =
    MappedColumnType.base[LocalDate, Date](Date.valueOf, _.toLocalDate)

  // Table definitions

  class UsersTable(tag: Tag) extends Table[User](tag, "users") {

    def username: Rep[String] = column[String](usernameName, O.PrimaryKey)

    def name: Rep[String] = column[String](nameName)


    override def * : ProvenShape[User] = (username, name) <> (User.tupled, User.unapply)

  }

  class UserPasswordsTable(tag: Tag) extends Table[UserPassword](tag, "user_passwords") {

    def username: Rep[String] = column[String](usernameName, O.PrimaryKey)

    def pwHash: Rep[String] = column[String]("password_hash")


    override def * : ProvenShape[UserPassword] = (username, pwHash) <> (UserPassword.tupled, UserPassword.unapply)

  }

  class LanguagesTable(tag: Tag) extends Table[Language](tag, "languages") {

    def id: Rep[Int] = column[Int](idName, O.PrimaryKey)

    def shortName: Rep[String] = column[String]("short_name")

    def name: Rep[String] = column[String](nameName)


    override def * : ProvenShape[Language] = (id, shortName, name) <> (Language.tupled, Language.unapply)

  }

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


    def * : ProvenShape[Bucket] = (id, distanceDays) <> (Bucket.tupled, Bucket.unapply)

  }

  class CollectionsTable(tag: Tag) extends Table[Collection](tag, "collections") {

    def id: Rep[Int] = column[Int](idName)

    def langId: Rep[Int] = column[Int]("lang_id")

    def name: Rep[String] = column[String](nameName)


    def pk: PrimaryKey = primaryKey("coll_pk", (id, langId))

    def langFk: ForeignKeyQuery[LanguagesTable, Language] = foreignKey("coll_language_fk", langId, languagesTQ)(_.id)


    override def * : ProvenShape[Collection] = (id, langId, name) <> (Collection.tupled, Collection.unapply)

  }

  class FlashcardsTable(tag: Tag) extends Table[Flashcard](tag, "flashcards") {

    def id: Rep[Int] = column[Int](idName)

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

    def id: Rep[Int] = column[Int](idName)

    def cardId: Rep[Int] = column[Int]("card_id")

    def collId: Rep[Int] = column[Int]("coll_id")

    def langId: Rep[Int] = column[Int]("lang_id")

    def answer: Rep[String] = column[String](answerName)

    def correctness: Rep[Correctness] = column[Correctness](correctnessName)


    def pk: PrimaryKey = primaryKey("ca_pk", (id, cardId, collId, langId))

    def cardFk: ForeignKeyQuery[FlashcardsTable, Flashcard] = foreignKey("ca_card_fk", (cardId, collId, langId), flashcardsTQ)(fc => (fc.id, fc.collId, fc.langId))


    override def * : ProvenShape[ChoiceAnswer] = (id, cardId, collId, langId, answer, correctness) <> (ChoiceAnswer.tupled, ChoiceAnswer.unapply)

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

    def cardId: Rep[Int] = column[Int](idName)

    def collId: Rep[Int] = column[Int]("coll_id")

    def langId: Rep[Int] = column[Int]("lang_id")

    def username: Rep[String] = column[String](usernameName)


    def pk: PrimaryKey = primaryKey("fcs_to_learn_pk", (cardId, collId, langId, username))

    def userFk: ForeignKeyQuery[UsersTable, User] = foreignKey("fcs_to_learn_user_fk", username, usersTQ)(_.username)

    def cardFk: ForeignKeyQuery[FlashcardsTable, Flashcard] = foreignKey("fcs_to_learn_card_fk", (cardId, collId, langId), flashcardsTQ)(fc => (fc.id, fc.collId, fc.langId))


    override def * : ProvenShape[(Int, Int, Int, String)] = (cardId, collId, langId, username)

  }

  class FlashcardsToRepeatView(tag: Tag) extends Table[(Int, Int, Int, String, Boolean, Int)](tag, "flashcards_to_repeat") {

    def cardId: Rep[Int] = column[Int](idName)

    def collId: Rep[Int] = column[Int]("coll_id")

    def langId: Rep[Int] = column[Int]("lang_id")

    def username: Rep[String] = column[String](usernameName)

    def correct: Rep[Boolean] = column[Boolean](correctName)

    def tries: Rep[Int] = column[Int](triesName)


    override def * : ProvenShape[(Int, Int, Int, String, Boolean, Int)] = (cardId, collId, langId, username, correct, tries)

  }

}
