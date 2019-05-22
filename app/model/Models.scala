package model

import java.time.LocalDate
import java.time.temporal.ChronoUnit

import enumeratum.{EnumEntry, PlayEnum}

import scala.collection.immutable

// User and password

final case class User(username: String, isAdmin: Boolean = false)

final case class UserPassword(username: String, pwHash: String)

// Course

final case class Course(id: Int, shortName: String, name: String)

final case class Collection(id: Int, courseId: Int, name: String)

// User <-> Course

final case class UserInCourse(username: String, courseId: Int)


// Flashcards

sealed trait CardType extends EnumEntry

case object CardType extends PlayEnum[CardType] {

  val values: immutable.IndexedSeq[CardType] = findValues

  case object Vocable extends CardType

  case object Text extends CardType

  case object Blank extends CardType

  case object Choice extends CardType

}

final case class Flashcard(
  cardId: Int, collId: Int, courseId: Int,
  cardType: CardType,
  question: String,
  questionHint: Option[String] = None,
  meaning: String = "",
  meaningHint: Option[String] = None,
  blanksAnswers: Seq[BlanksAnswerFragment] = Seq.empty,
  choiceAnswers: Seq[ChoiceAnswer] = Seq.empty
) {

  def identifier: FlashcardIdentifier = FlashcardIdentifier(cardId, collId, courseId)

}

sealed trait FlashcardComponent {
  val answerId: Int
  val cardId  : Int
  val collId  : Int
  val courseId: Int
}

// Blanks

final case class BlanksAnswerFragment(answerId: Int, cardId: Int, collId: Int, courseId: Int, answer: String, isAnswer: Boolean) extends FlashcardComponent

// Single and Multiple choice

final case class ChoiceAnswer(answerId: Int, cardId: Int, collId: Int, courseId: Int, answer: String, correctness: Correctness) extends FlashcardComponent

sealed trait Correctness extends EnumEntry

case object Correctness extends PlayEnum[Correctness] {

  val values: immutable.IndexedSeq[Correctness] = findValues

  case object Correct extends Correctness

  case object Optional extends Correctness

  case object Wrong extends Correctness

}

final case class FlashcardIdentifier(cardId: Int, collId: Int, courseId: Int) {

  def asString = s"$courseId.$collId.$cardId"

}

// User answered flashcard

final case class UserAnsweredFlashcard(username: String, cardId: Int, collId: Int, courseId: Int, bucket: Int, dateAnswered: LocalDate, correct: Boolean, tries: Int) {

  //  def cardIdentifier: FlashcardIdentifier = FlashcardIdentifier(cardId, collId, courseId)

  def isActive: Boolean = dateAnswered.until(LocalDate.now(), ChronoUnit.DAYS) < Math.pow(3, bucket - 1)

}
