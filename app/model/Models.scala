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

// User <-> Course

final case class UserInCourse(username: String, courseId: String)

// Collection

final case class Collection(id: Int, name: String)

// Course <-> Collection

final case class CollectionInCourse(collId: Int, courseId: String)

// Languages and Collections

// TODO: remove Languague?
final case class Language(id: Int, name: String)

final case class Bucket(id: Int, distanceDays: Int)

// Flashcards

sealed trait CardType extends EnumEntry

case object CardType extends PlayEnum[CardType] {

  val values: immutable.IndexedSeq[CardType] = findValues

  case object Vocable extends CardType

  case object Text extends CardType

  case object Blank extends CardType

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

final case class FlashcardIdentifier(cardId: Int, collId: Int) {

  def asString = s"$collId.$cardId"

}

// User answered flashcard

final case class UserAnsweredFlashcard(username: String, cardId: Int, collId: Int, bucketId: Int, dateAnswered: LocalDate, correct: Boolean, tries: Int) {

  def isActive: Boolean = dateAnswered.until(LocalDate.now(), ChronoUnit.DAYS) < Math.pow(3, bucketId - 1)

}