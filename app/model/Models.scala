package model

import java.time.LocalDate
import java.time.temporal.ChronoUnit

import enumeratum.{EnumEntry, PlayEnum}

import scala.collection.immutable

// User and password

final case class User(username: String, isAdmin: Boolean = false)

final case class UserPassword(username: String, pwHash: String)

// Course

final case class Course(id: String, name: String)

// Languages and Collections

final case class Language(id: Int, name: String)

final case class Collection(id: Int, langId: Int, name: String)

final case class Bucket(id: Int, distanceDays: Int)

// Flashcards

sealed trait CardType extends EnumEntry

case object CardType extends PlayEnum[CardType] {

  val values: immutable.IndexedSeq[CardType] = findValues

  case object Vocable extends CardType

  case object Text extends CardType

  case object SingleChoice extends CardType

  case object MultipleChoice extends CardType

}

sealed trait Correctness extends EnumEntry

case object Correctness extends PlayEnum[Correctness] {

  val values: immutable.IndexedSeq[Correctness] = findValues

  case object Correct extends Correctness

  case object Optional extends Correctness

  case object Wrong extends Correctness

}

final case class FlashcardIdentifier(cardId: Int, collId: Int, langId: Int) {

  def asString = s"$langId.$collId.$cardId"

}

final case class Flashcard(id: Int, collId: Int, langId: Int, cardType: CardType, question: String, meaning: Option[String]) {

  def identifier: FlashcardIdentifier = FlashcardIdentifier(id, collId, langId)

}

final case class ChoiceAnswer(id: Int, cardId: Int, collId: Int, langId: Int, answer: String, correctness: Correctness)

final case class CompleteFlashcard(flashcard: Flashcard, choiceAnswers: Seq[ChoiceAnswer]) {

  def id: Int = flashcard.id

  def collId: Int = flashcard.collId

  def langId: Int = flashcard.langId

  def cardType: CardType = flashcard.cardType

  def question: String = flashcard.question

  def meaning: Option[String] = flashcard.meaning

}

// User answered flashcard

final case class UserAnsweredFlashcard(username: String, cardId: Int, collId: Int, langId: Int, bucketId: Int, dateAnswered: LocalDate, correct: Boolean, tries: Int) {

  def isActive: Boolean = dateAnswered.until(LocalDate.now(), ChronoUnit.DAYS) < Math.pow(3, bucketId - 1)

}