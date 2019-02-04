package model

import model.levenshtein.{EditOperation, Levenshtein}

final case class CorrectionResult(correct: Boolean, cardType: CardType, learnerSolution: Solution, operations: Seq[EditOperation],
                                  answersSelection: Option[AnswerSelectionResult], newTriesCount: Int, maybeSampleSolution: Option[String] = None)

final case class AnswerSelectionResult(wrong: Seq[Int], correct: Seq[Int], missing: Seq[Int]) {
  def isCorrect: Boolean = wrong.isEmpty && missing.isEmpty
}


object Corrector {


  private def matchAnswerIds(selectedIds: Seq[Int], correctIds: Seq[Int]): AnswerSelectionResult = AnswerSelectionResult(
    wrong = selectedIds diff correctIds,
    correct = selectedIds intersect correctIds,
    missing = correctIds diff selectedIds
  )

  private def correctTextualFlashcard(flashcard: CompleteFlashcard, solution: Solution, previousTriesCount: Int): Seq[EditOperation] = flashcard.meaning match {
    case None          => ???
    case Some(meaning) => Levenshtein.calculateBacktrace(solution.solution, meaning)
  }

  private def correctChoiceFlashcard(flashcard: CompleteFlashcard, solution: Solution, previousTriesCount: Int): AnswerSelectionResult = {

    val selectedAnswerIds: Seq[Int] = solution.selectedAnswers
    val correctAnswerIds: Seq[Int] = flashcard.choiceAnswers.filter(_.correctness != Correctness.Wrong).map(_.id)

    matchAnswerIds(selectedAnswerIds, correctAnswerIds)
  }


  def correct(completeFlashcard: CompleteFlashcard, solution: Solution, previousTriesCount: Int): CorrectionResult = {
    val (correct, editOps, ansSelection) = completeFlashcard.cardType match {
      case CardType.Vocable | CardType.Text =>

        val editOperations = correctTextualFlashcard(completeFlashcard, solution, previousTriesCount)

        (editOperations.isEmpty, editOperations, None)

      case CardType.SingleChoice | CardType.MultipleChoice =>

        val answerSelectionResult = correctChoiceFlashcard(completeFlashcard, solution, previousTriesCount)

        (answerSelectionResult.isCorrect, Seq[EditOperation](), Some(answerSelectionResult))
    }

    val newTriesCount: Int = if (correct) previousTriesCount else previousTriesCount + 1

    val maybeSampleSolution: Option[String] = if (newTriesCount >= 2) {
      completeFlashcard.meaning
    } else None

    CorrectionResult(correct, completeFlashcard.cardType, solution, editOps, ansSelection, newTriesCount, maybeSampleSolution)
  }

}