package model

import java.time.LocalDate
import java.time.temporal.ChronoUnit

import enumeratum.{EnumEntry, PlayEnum}
import model.JsonFormats.{blanksAnswerFragmentFormat, choiceAnswerFormat}
import play.api.Logger
import play.api.libs.json._

import scala.collection.immutable

// User and password

final case class User(username: String, hasAcceptedDataPrivacyStatement: Boolean = false, isAdmin: Boolean = false)

final case class UserPassword(username: String, pwHash: String)

// Course

final case class Course(id: Int, shortName: String, name: String)

final case class Language(id: Int, name: String)

final case class Collection(id: Int, courseId: Int, startLanguage: Language, targetLanguage: Language, name: String)

// User <-> Course

final case class UserInCourse(username: String, courseId: Int)

// Flashcards

sealed trait CardType extends EnumEntry

case object CardType extends PlayEnum[CardType] {

  val values: immutable.IndexedSeq[CardType] = findValues

  case object Word extends CardType

  case object Text extends CardType

  case object Blank extends CardType

  case object Choice extends CardType

}

final case class Flashcard(
  cardId: Int, collId: Int, courseId: Int,
  cardType: CardType,
  frontsJson: JsValue,
  frontHint: Option[String] = None,
  backsJson: JsValue = JsArray(),
  backHint: Option[String] = None,
  blanksAnswerFragmentsJson: JsValue = JsArray(),
  choiceAnswersJson: JsValue = JsArray()
) {

  private def logger = Logger(classOf[Flashcard])

  private def convertFromJson[T](jsValue: JsValue, reads: Reads[T]): Seq[T] = Json.fromJson(jsValue)(Reads.seq(reads)) match {
    case JsSuccess(value, _) => value
    case JsError(errors)     =>
      logger.error("Error while converting json in Flashcard: " + errors.map("\t" + _.toString()).mkString("\n"))
      Seq.empty
  }

  val onlyFrontToBack: Boolean = cardType == CardType.Choice || cardType == CardType.Blank

  def fronts: Seq[String] = convertFromJson(frontsJson, {
    case JsString(value) => JsSuccess(value)
    case _               => JsError()
  })

  def backs: Seq[String] = convertFromJson(backsJson, {
    case JsString(value) => JsSuccess(value)
    case _               => JsError()
  })

  def blanksAnswerFragments: Seq[BlanksAnswerFragment] = convertFromJson(blanksAnswerFragmentsJson, blanksAnswerFragmentFormat)

  def choiceAnswers: Seq[ChoiceAnswer] = convertFromJson(choiceAnswersJson, choiceAnswerFormat)

}

// Blanks

final case class BlanksAnswerFragment(answerId: Int, answer: String, isAnswer: Boolean)

// Single and Multiple choice

final case class ChoiceAnswer(answerId: Int, answer: String, correctness: Correctness)

sealed trait Correctness extends EnumEntry

case object Correctness extends PlayEnum[Correctness] {

  val values: immutable.IndexedSeq[Correctness] = findValues

  case object Correct extends Correctness

  case object Optional extends Correctness

  case object Wrong extends Correctness

}

// User answered flashcard

final case class UserAnsweredFlashcard(
  username: String,
  cardId: Int,
  collId: Int,
  courseId: Int,
  cardType: CardType,
  bucket: Int,
  dateAnswered: LocalDate,
  correct: Boolean,
  wrongTries: Int,
  frontToBack: Boolean
) {

  lazy val isActive: Boolean = dateAnswered.until(LocalDate.now(), ChronoUnit.DAYS) < Math.pow(3, bucket - 1)

}
