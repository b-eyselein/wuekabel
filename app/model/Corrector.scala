package model

import model.levenshtein.{EditOperation, Levenshtein}

final case class CorrectionResult(correct: Boolean, cardType: CardType, learnerSolution: Solution, operations: Seq[EditOperation], answersSelection: Option[AnswerSelectionResult])

final case class AnswerSelectionResult(wrong: Seq[Int], correct: Seq[Int], missing: Seq[Int]) {
  def isCorrect: Boolean = wrong.isEmpty && missing.isEmpty
}

object Corrector {

  def matchAnswerIds(selectedIds: Seq[Int], correctIds: Seq[Int]): AnswerSelectionResult = AnswerSelectionResult(
    wrong = selectedIds diff correctIds,
    correct = selectedIds intersect correctIds,
    missing = correctIds diff selectedIds
  )


  def correct(completeFlashcard: CompleteFlashcard, solution: Solution): CorrectionResult = completeFlashcard.flashcard.cardType match {
    case CardType.Vocable | CardType.Text                =>
      completeFlashcard.flashcard.meaning match {
        case None          => ???
        case Some(meaning) =>
          val editOperations = Levenshtein.calculateBacktrace(solution.solution, meaning)
          CorrectionResult(editOperations.isEmpty, completeFlashcard.flashcard.cardType, solution, editOperations, None)
      }
    case CardType.SingleChoice | CardType.MultipleChoice =>
      val selectedAnswerIds = solution.selectedAnswers
      val correctAnswerIds = completeFlashcard.choiceAnswers.filter(_.correctness != Correctness.Wrong).map(_.id)

      val answerSelectionResult = matchAnswerIds(selectedAnswerIds, correctAnswerIds)

      CorrectionResult(answerSelectionResult.isCorrect, completeFlashcard.flashcard.cardType, solution, Seq[EditOperation](), Some(answerSelectionResult))
  }

}