package model

import java.time.LocalDate
import java.time.temporal.ChronoUnit

import enumeratum.{EnumEntry, PlayEnum}

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
  front: String,
  frontHint: Option[String] = None,
  back: String = "",
  backHint: Option[String] = None,
  blanksAnswers: Seq[BlanksAnswerFragment] = Seq.empty,
  choiceAnswers: Seq[ChoiceAnswer] = Seq.empty
)

sealed trait FlashcardComponent {
  val answerId: Int
}

// Blanks

final case class BlanksAnswerFragment(answerId: Int, answer: String, isAnswer: Boolean) extends FlashcardComponent

// Single and Multiple choice

final case class ChoiceAnswer(answerId: Int, answer: String, correctness: Correctness) extends FlashcardComponent

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
