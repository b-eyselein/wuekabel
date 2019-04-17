package model

import model.levenshtein.Levenshtein

object Corrector {

  def matchAnswerIds(selectedIds: Seq[Int], correctIds: Seq[Int]): AnswerSelectionResult = AnswerSelectionResult(
    wrong = selectedIds diff correctIds,
    correct = selectedIds intersect correctIds,
    missing = correctIds diff selectedIds
  )

  private def correctBlanksFlashcard(blanksFlashcard: Flashcard, solution: Solution): Boolean = ???

  private def correctChoiceFlashcard(flashcard: Flashcard, solution: Solution): AnswerSelectionResult = {

    val selectedAnswerIds: Seq[Int] = solution.selectedAnswers
    val correctAnswerIds: Seq[Int] = flashcard.choiceAnswers.filter(_.correctness != Correctness.Wrong).map(_.answerId)

    matchAnswerIds(selectedAnswerIds, correctAnswerIds)
  }


  def correct(completeFlashcard: Flashcard, solution: Solution): CorrectionResult =
    completeFlashcard.cardType match {
      case CardType.Vocable | CardType.Text =>
        val editOperations = Levenshtein.calculateBacktrace(solution.solution, completeFlashcard.meaning)
        CorrectionResult(editOperations.isEmpty, operations = editOperations)

      case CardType.Blank =>
        val correct = correctBlanksFlashcard(completeFlashcard, solution)
        CorrectionResult(correct)

      case CardType.Choice =>
        val answerSelectionResult = correctChoiceFlashcard(completeFlashcard, solution)
        CorrectionResult(answerSelectionResult.isCorrect, answersSelection = Some(answerSelectionResult))

    }

}
