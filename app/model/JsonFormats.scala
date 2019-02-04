package model

import play.api.libs.json._
import Consts._
import model.levenshtein._
import play.api.libs.functional.syntax._


final case class Solution(solution: String, selectedAnswers: Seq[Int])

object JsonFormats {

  private val solutionWrites: Writes[Solution] = (
    (__ \ learnerSolutionName).write[String] and
      (__ \ selectedAnswersName).write[Seq[Int]]
    ) (unlift(Solution.unapply))

  private val solutionReads: Reads[Solution] = (
    (__ \ learnerSolutionName).read[String] and
      (__ \ selectedAnswersName).read[Seq[Int]]
    ) (Solution.apply _)

  val solutionFormat: Format[Solution] = Format(solutionReads, solutionWrites)

  private implicit val charWrites: Writes[Char] = x => JsString(x.toString)

  private implicit val editOperationWrites: Writes[EditOperation] = (
    (__ \ operationTypeName).write[OperationType](OperationType.jsonFormat) and
      (__ \ indexName).write[Int] and
      (__ \ charName).writeNullable[Char]
    ) (unlift(EditOperation.unapply))

  private implicit val answerSelectionResultWrites: Writes[AnswerSelectionResult] = (
    (__ \ wrongName).write[Seq[Int]] and
      (__ \ correctName).write[Seq[Int]] and
      (__ \ missingName).write[Seq[Int]]
    ) (unlift(AnswerSelectionResult.unapply))

  def correctionResultWrites: Writes[CorrectionResult] = (
    (__ \ correctName).write[Boolean] and
      (__ \ cardTypeName).write[CardType] and
      (__ \ learnerSolutionName).write[Solution](solutionFormat) and
      (__ \ operationsName).write[Seq[EditOperation]] and
      (__ \ answerSelectionName).write[Option[AnswerSelectionResult]] and
      (__ \ newTriesCountName).write[Int] and
      (__ \ maybeSampleSolName).write[Option[String]]
    ) (unlift(CorrectionResult.unapply))

}
