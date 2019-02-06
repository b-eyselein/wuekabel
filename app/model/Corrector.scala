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

  private def correctWordFlashcard(wordFlashcard: WordFlashcard, solution: Solution, previousTriesCount: Int): Seq[EditOperation] =
    Levenshtein.calculateBacktrace(solution.solution, wordFlashcard.meaning)

  private def correctTextFlashcard(textFlashcard: TextFlashcard, solution: Solution, previousTriesCount: Int): Seq[EditOperation] =
    Levenshtein.calculateBacktrace(solution.solution, textFlashcard.meaning)

  private def correctBlanksFlashcard(blanksFlashcard: BlanksFlashcard, solution: Solution, previousTriesCount: Int): Boolean = ???

  private def correctChoiceFlashcard(flashcard: ChoiceFlashcard, solution: Solution, previousTriesCount: Int): AnswerSelectionResult = {

    val selectedAnswerIds: Seq[Int] = solution.selectedAnswers
    val correctAnswerIds: Seq[Int] = flashcard.choiceAnswers.filter(_.correctness != Correctness.Wrong).map(_.answerId)

    matchAnswerIds(selectedAnswerIds, correctAnswerIds)
  }


  def correct(completeFlashcard: Flashcard, solution: Solution, previousTriesCount: Int): CorrectionResult = {
    val (correct, editOps, ansSelection) = completeFlashcard match {
      case wfc: WordFlashcard =>

        val editOperations = correctWordFlashcard(wfc, solution, previousTriesCount)
        (editOperations.isEmpty, editOperations, None)


      case tfc: TextFlashcard =>

        val editOperations = correctTextFlashcard(tfc, solution, previousTriesCount)
        (editOperations.isEmpty, editOperations, None)

      case bfc: BlanksFlashcard =>

        val correct = correctBlanksFlashcard(bfc, solution, previousTriesCount)

        (correct, Seq[EditOperation](), None)

      case cfc: ChoiceFlashcard =>

        val answerSelectionResult = correctChoiceFlashcard(cfc, solution, previousTriesCount)

        (answerSelectionResult.isCorrect, Seq[EditOperation](), Some(answerSelectionResult))
    }

    val newTriesCount: Int = if (correct) previousTriesCount else previousTriesCount + 1

    val maybeSampleSolution: Option[String] = if (newTriesCount >= 2) {
      None //completeFlashcard.meaning
    } else None

    CorrectionResult(correct, completeFlashcard.cardType, solution, editOps, ansSelection, newTriesCount, maybeSampleSolution)
  }

}