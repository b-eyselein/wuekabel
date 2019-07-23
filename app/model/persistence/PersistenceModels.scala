package model.persistence

import model.JsonFormats.{blanksAnswerFragmentFormat, choiceAnswerFormat}
import model._
import play.api.libs.json._

object PersistenceModels {

  def dbFlashcardToFlashcard(flashcard: DBFlashcard): Flashcard = flashcard match {
    case DBFlashcard(cardId, collId, courseId, cardType, front, frontHint, back, backHint, blanksAnswerFragmentsJsValue, choiceAnswersJsValue) =>

      implicit val caf: Format[ChoiceAnswer] = choiceAnswerFormat
      val choiceAnswers: Seq[ChoiceAnswer] = Json.fromJson[Seq[ChoiceAnswer]](choiceAnswersJsValue) match {
        case JsSuccess(cas, _) => cas
        case JsError(errors)   => ???
      }

      implicit val baff: Format[BlanksAnswerFragment] = blanksAnswerFragmentFormat
      val blanksAnswerFragments: Seq[BlanksAnswerFragment] = Json.fromJson[Seq[BlanksAnswerFragment]](blanksAnswerFragmentsJsValue) match {
        case JsSuccess(bafs, _) => bafs
        case JsError(errors)    => ???
      }

      Flashcard(cardId, collId, courseId, cardType, front, frontHint, back, backHint, blanksAnswerFragments, choiceAnswers)
  }


  def collFromDbColl(dbValues: (CollectionBasics, Language, Language)): Collection = dbValues match {
    case (dbColl, frontLang, backLang) => Collection(dbColl.collectionId, dbColl.courseId, frontLang, backLang, dbColl.name)
  }

  def flashcardToDbFlashcard(fc: Flashcard): DBFlashcard = fc match {
    case Flashcard(cardId, collId, courseId, cardType, front, frontHint, back, backHint, choiceAnswers, blanksAnswerFragments) =>

      implicit val caf: Format[ChoiceAnswer] = choiceAnswerFormat
      implicit val baff: Format[BlanksAnswerFragment] = blanksAnswerFragmentFormat

      DBFlashcard(cardId, collId, courseId, cardType, front, frontHint, back, backHint,
        blanksAnswerFragmentsJsValue = Json.toJson(blanksAnswerFragments),
        choiceAnswersJsValue = Json.toJson(choiceAnswers))
  }

}

final case class DBFlashcard(
  cardId: Int,
  collId: Int,
  courseId: Int,
  cardType: CardType,
  front: String,
  frontHint: Option[String],
  back: String,
  backHint: Option[String],
  blanksAnswerFragmentsJsValue: JsValue,
  choiceAnswersJsValue: JsValue
)

final case class FlashcardToAnswerData(cardId: Int, collId: Int, courseId: Int, username: String, frontToBack: Boolean)
