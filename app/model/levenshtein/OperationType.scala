package model.levenshtein

import enumeratum.{EnumEntry, PlayEnum}

import scala.collection.immutable


sealed trait OperationType extends EnumEntry

object OperationType extends PlayEnum[OperationType] {

  val values: immutable.IndexedSeq[OperationType] = findValues

  case object Delete extends OperationType

  case object Insert extends OperationType

  case object Replace extends OperationType

}


final case class EditOperation(operationType: OperationType, index: Int, char: Option[Char] = None)