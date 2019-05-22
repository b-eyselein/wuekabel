package model

import model.levenshtein._
import play.api.libs.json._


object JsonFormats {

  // Incoming

  val solutionFormat: Format[Solution] = Json.format[Solution]

  // Result

  private implicit val charFormat: Format[Char] = Format({
    case JsString(str) => JsSuccess(str.charAt(0))
    case _             => JsError("")
  }, x => JsString(x.toString))

  private implicit val operationTypeFormat: Format[OperationType] = OperationType.jsonFormat

  private implicit val editOperationFormat: Format[EditOperation] = Json.format[EditOperation]

  private implicit val answerSelectionResultFormat: Format[AnswerSelectionResult] = Json.format[AnswerSelectionResult]

  val completeCorrectionResultFormat: Format[CorrectionResult] = Json.format[CorrectionResult]

  // Flashcard

  private implicit val choiceAnswerFormat: Format[ChoiceAnswer] = Json.format[ChoiceAnswer]

  private implicit val blanksAnswerFragmentFormat: Format[BlanksAnswerFragment] = Json.format[BlanksAnswerFragment]

  val flashcardFormat: Format[Flashcard] = Json.format[Flashcard]

}
