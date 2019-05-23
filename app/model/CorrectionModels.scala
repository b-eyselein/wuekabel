package model

import model.levenshtein.EditOperation

final case class Solution(
  cardId: Int,
  collId: Int,
  courseId: Int,
  solution: String,
  selectedAnswers: Seq[Int],
  frontToBack: Boolean
)

final case class CorrectionResult(
  correct: Boolean,
  operations: Seq[EditOperation] = Seq.empty,
  answersSelection: Option[AnswerSelectionResult] = None,
  newTriesCount: Int = 0,
  maybeSampleSolution: Option[String] = None
)

final case class AnswerSelectionResult(wrong: Seq[Int], correct: Seq[Int], missing: Seq[Int]) {
  def isCorrect: Boolean = wrong.isEmpty && missing.isEmpty
}

