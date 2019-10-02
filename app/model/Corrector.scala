package model

import java.time.LocalDate

import model.Consts.multipleSolutionsSplitChar

object Corrector {

  private val maxBucketId: Int = 6

  // Blanks flashcards

  private def correctBlanksFlashcard(blanksFlashcard: Flashcard, solution: Solution): Boolean = ???

  // Choice flashcards

  def matchAnswerIds(selectedIds: Seq[Int], correctIds: Seq[Int]): AnswerSelectionResult = AnswerSelectionResult(
    wrong = selectedIds diff correctIds,
    correct = selectedIds intersect correctIds,
    missing = correctIds diff selectedIds
  )

  private def correctChoiceFlashcard(flashcard: Flashcard, solution: Solution): AnswerSelectionResult = {

    val selectedAnswerIds: Seq[Int] = solution.selectedAnswers
    val correctAnswerIds : Seq[Int] = flashcard.choiceAnswers.filter(_.correctness != Correctness.Wrong).map(_.answerId)

    matchAnswerIds(selectedAnswerIds, correctAnswerIds)
  }

  // Textual flashcards

  private def correctTextualFlashcard(flashcard: Flashcard, solution: Solution): MatchingResult = {

    val sampleSolutions = if (solution.frontToBack) flashcard.backs else flashcard.fronts

    Matcher.doMatch(solution.solutions.toList, sampleSolutions.toList)

  }

  // Complete correction

  private def correctFlashcard(flashcard: Flashcard, solution: Solution): CorrectionResult = flashcard.cardType match {
    case CardType.Word | CardType.Text =>
      val correctionResult = correctTextualFlashcard(flashcard, solution)
      CorrectionResult(correctionResult.isCorrect, matchingResult = Some(correctionResult))

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
          UserAnsweredFlashcard(user.username, flashcard.cardId, flashcard.collId, flashcard.courseId, flashcard.cardType,
            bucket = 0, today, isCorrect, wrongTries = newTries, solution.frontToBack)
        )

      case Some(oldAnswer) =>

        val newBucket = Math.min(if (isCorrect) oldAnswer.bucket + 1 else oldAnswer.bucket, maxBucketId)

        val triesToAdd = if (isCorrect) 0 else 1
        val oldTries   = if (oldAnswer.isActive) oldAnswer.wrongTries else 0

        val newWrongTriesCount = oldTries + triesToAdd

        val maybeSampleSolution = if (newWrongTriesCount < 2) {
          None
        } else if (solution.frontToBack) {
          Some(flashcard.backs.mkString(multipleSolutionsSplitChar))
        } else {
          Some(flashcard.fronts.mkString(multipleSolutionsSplitChar))
        }

        (
          correctionResult.copy(newTriesCount = newWrongTriesCount, maybeSampleSolution = maybeSampleSolution),
          oldAnswer.copy(bucket = newBucket, dateAnswered = today, correct = isCorrect, wrongTries = newWrongTriesCount)
        )
    }
  }

}
