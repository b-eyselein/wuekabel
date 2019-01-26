package model

import java.time.LocalDate

import enumeratum.{EnumEntry, PlayEnum}

import scala.collection.immutable

// User and password

final case class User(username: String, name: String)

final case class UserPassword(username: String, pwHash: String)

// Languages and Collections

final case class Language(id: Int, shortName: String, name: String)

final case class Collection(id: Int, langId: Int, name: String)

final case class Bucket(id: Int, distanceDays: Int)

// Flashcards

sealed trait CardType extends EnumEntry

case object CardType extends PlayEnum[CardType] {

  val values: immutable.IndexedSeq[CardType] = findValues

  case object VocableCard extends CardType

  case object TextCard extends CardType

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

final case class Flashcard(id: Int, collId: Int, langId: Int, cardType: CardType, question: String, meaning: Option[String])

final case class ChoiceAnswer(id: Int, cardId: Int, collId: Int, langId: Int, answer: String, correctness: Correctness)

// User answered flashcard

final case class UserAnsweredFlashcard(username: String, cardId: Int, collId: Int, langId: Int, bucketId: Int, dateAnswered: LocalDate, correct: Boolean, tries: Int)