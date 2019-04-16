package model

import model.levenshtein.EditOperation

final case class Solution(solution: String, selectedAnswers: Seq[Int])

final case class CorrectionResult(
  correct: Boolean,
  operations: Seq[EditOperation] = Seq.empty,
  answersSelection: Option[AnswerSelectionResult] = None
)


final case class CompleteCorrectionResult(
  correct: Boolean,
  operations: Seq[EditOperation],
  answersSelection: Option[AnswerSelectionResult],
  newTriesCount: Int,
  maybeSampleSolution: Option[String] = None
)

final case class AnswerSelectionResult(wrong: Seq[Int], correct: Seq[Int], missing: Seq[Int]) {
  def isCorrect: Boolean = wrong.isEmpty && missing.isEmpty
}

