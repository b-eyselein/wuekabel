package model

import model.levenshtein._
import play.api.libs.json._
import play.api.libs.functional.syntax._

object JsonFormats {

  // Incoming

  private val stringSolutionFormat: Format[StringSolution] = Json.format[StringSolution]

  val solutionFormat: Format[Solution] = {
    implicit val ssf: Format[StringSolution] = stringSolutionFormat

    Json.format[Solution]
  }

  // Result

  private val charFormat: Format[Char] = Format({
    case JsString(str) => JsSuccess(str.charAt(0))
    case _             => JsError("")
  }, x => JsString(x.toString))

  private val operationTypeFormat: Format[OperationType] = OperationType.jsonFormat

  private val editOperationFormat: Format[EditOperation] = {
    implicit val cf: Format[Char] = charFormat

    implicit val otf: Format[OperationType] = operationTypeFormat

    Json.format[EditOperation]
  }

  private val levenshteinDistanceWrites: Writes[LevenshteinDistance] = {
    def unapplyLevenshteinDistance: LevenshteinDistance => (String, String, Int) = ld => (ld.start, ld.target, ld.distance)

    (
      (__ \ "start").write[String] and
        (__ \ "target").write[String] and
        (__ \ "distance").write[Int]
      ) (unapplyLevenshteinDistance)
  }

  private val matchingResultWrites: Writes[MatchingResult] = {
    implicit val eof: Format[EditOperation] = editOperationFormat

    implicit val ldf: Writes[LevenshteinDistance] = levenshteinDistanceWrites

    Json.writes[MatchingResult]
  }

  private val answerSelectionResultFormat: Format[AnswerSelectionResult] = Json.format[AnswerSelectionResult]

  val completeCorrectionResultWrites: Writes[CorrectionResult] = {
    implicit val asrf: Format[AnswerSelectionResult] = answerSelectionResultFormat

    implicit val mrf: Writes[MatchingResult] = matchingResultWrites

    Json.writes[CorrectionResult]
  }

  // FlashcardToAnswer

  val choiceAnswerFormat: Format[ChoiceAnswer] = Json.format[ChoiceAnswer]

  val blanksAnswerFragmentFormat: Format[BlanksAnswerFragment] = Json.format[BlanksAnswerFragment]

  val flashcardFormat: Format[Flashcard] = {
    implicit val caf: Format[ChoiceAnswer] = choiceAnswerFormat

    implicit val baff: Format[BlanksAnswerFragment] = blanksAnswerFragmentFormat

    Json.format[Flashcard]
  }

  val flashcardToAnswerFormat: Format[FlashcardToAnswer] = {
    implicit val fcf: Format[Flashcard] = flashcardFormat

    Json.format[FlashcardToAnswer]
  }

}
