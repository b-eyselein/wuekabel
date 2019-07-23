package model

import model.levenshtein._
import play.api.libs.json._


object JsonFormats {

  // Incoming

  val solutionFormat: Format[Solution] = Json.format[Solution]

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

  private val answerSelectionResultFormat: Format[AnswerSelectionResult] = Json.format[AnswerSelectionResult]

  val completeCorrectionResultFormat: Format[CorrectionResult] = {
    implicit val eof: Format[EditOperation] = editOperationFormat

    implicit val asrf: Format[AnswerSelectionResult] = answerSelectionResultFormat

    Json.format[CorrectionResult]
  }

  // Flashcard

  val choiceAnswerFormat: Format[ChoiceAnswer] = Json.format[ChoiceAnswer]

  val blanksAnswerFragmentFormat: Format[BlanksAnswerFragment] = Json.format[BlanksAnswerFragment]

  val flashcardToAnswerFormat: Format[FlashcardToAnswer] = {
    implicit val caf: Format[ChoiceAnswer] = choiceAnswerFormat

    implicit val baff: Format[BlanksAnswerFragment] = blanksAnswerFragmentFormat

    Json.format[FlashcardToAnswer]
  }

}
