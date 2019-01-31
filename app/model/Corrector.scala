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

  def correctTextualFlashcard(flashcard: Flashcard, solution: Solution): CorrectionResult = flashcard.meaning match {
    case None          => ???
    case Some(meaning) =>
      val editOperations: Seq[EditOperation] = Levenshtein.calculateBacktrace(solution.solution, meaning)

      CorrectionResult(editOperations.isEmpty, flashcard.cardType, solution, editOperations, None)
  }

  def correctChoiceFlashcard(flashcard: Flashcard, answers: Seq[ChoiceAnswer], solution: Solution): CorrectionResult = {

    val selectedAnswerIds: Seq[Int] = solution.selectedAnswers
    val correctAnswerIds: Seq[Int] = answers.filter(_.correctness != Correctness.Wrong).map(_.id)

    val answerSelectionResult = matchAnswerIds(selectedAnswerIds, correctAnswerIds)

    CorrectionResult(answerSelectionResult.isCorrect, flashcard.cardType, solution, Seq[EditOperation](), Some(answerSelectionResult))
  }


  def correct(completeFlashcard: CompleteFlashcard, solution: Solution): CorrectionResult = completeFlashcard.flashcard.cardType match {
    case CardType.Vocable | CardType.Text                => correctTextualFlashcard(completeFlashcard.flashcard, solution)
    case CardType.SingleChoice | CardType.MultipleChoice => correctChoiceFlashcard(completeFlashcard.flashcard, completeFlashcard.choiceAnswers, solution)
  }

}