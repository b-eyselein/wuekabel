package model

import java.time.LocalDate

import model.levenshtein.Levenshtein

object Corrector {

  private val maxBucketId: Int = 6

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

  private def correctFlashcard(flashcard: Flashcard, solution: Solution): CorrectionResult = flashcard.cardType match {
    case CardType.Vocable | CardType.Text =>
      val sampleSolution = if (solution.frontToBack) flashcard.back else flashcard.front
      val editOperations = Levenshtein.calculateBacktrace(solution.solution, sampleSolution)
      CorrectionResult(editOperations.isEmpty, operations = editOperations)

    case CardType.Blank =>
      val correct = correctBlanksFlashcard(flashcard, solution)
      CorrectionResult(correct)

    case CardType.Choice =>
      val answerSelectionResult = correctChoiceFlashcard(flashcard, solution)
      CorrectionResult(answerSelectionResult.isCorrect, answersSelection = Some(answerSelectionResult))
  }

  def completeCorrect(user: User, solution: Solution, flashcard: Flashcard, maybePreviousDbAnswer: Option[UserAnsweredFlashcard]): (CorrectionResult, UserAnsweredFlashcard) = {

    val correctionResult = correctFlashcard(flashcard, solution)

    val isCorrect = correctionResult.correct

    val today = LocalDate.now()

    maybePreviousDbAnswer match {
      case None =>
        val newTries = if (isCorrect) 0 else 1
        (
          correctionResult.copy(newTriesCount = newTries),
          UserAnsweredFlashcard(user.username, flashcard.cardId, flashcard.collId, flashcard.courseId, bucket = 0, today, isCorrect, wrongTries = newTries, solution.frontToBack)
        )

      case Some(oldAnswer) =>

        val newBucket = Math.min(if (isCorrect) oldAnswer.bucket + 1 else oldAnswer.bucket, maxBucketId)

        val triesToAdd = if (isCorrect) 0 else 1
        val oldTries = if (oldAnswer.isActive) oldAnswer.wrongTries else 0

        val newTries = oldTries + triesToAdd

        (
          correctionResult.copy(newTriesCount = newTries, maybeSampleSolution = None),
          oldAnswer.copy(bucket = newBucket, dateAnswered = today, correct = isCorrect, wrongTries = newTries)
        )
    }
  }

}
