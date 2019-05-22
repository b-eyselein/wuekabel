package model

import java.time.LocalDate
import java.time.temporal.ChronoUnit

import model.levenshtein.Levenshtein

import scala.util.{Failure, Success, Try}

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

  private def correctFlashcard(completeFlashcard: Flashcard, solution: Solution): CorrectionResult = completeFlashcard.cardType match {
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

  def completeCorrect(user: User, solution: Solution, flashcard: Flashcard, maybePreviousDbAnswer: Option[UserAnsweredFlashcard]): Try[(CorrectionResult, UserAnsweredFlashcard)] = {


    val correctionResult = correctFlashcard(flashcard, solution)

    val isCorrect = correctionResult.correct

    val today = LocalDate.now()

    maybePreviousDbAnswer match {
      case None =>
        val newTries = 0
        Success((
          correctionResult.copy(newTriesCount = newTries),
          UserAnsweredFlashcard(user.username, flashcard.cardId, flashcard.collId, flashcard.courseId, bucket = 0, today, isCorrect, tries = newTries)
        ))

      case Some(oldAnswer) =>

        val daysSinceLastAnswer: Long = Math.abs(ChronoUnit.DAYS.between(today, oldAnswer.dateAnswered))

        val isTryInNewBucket = daysSinceLastAnswer >= Math.pow(3, oldAnswer.bucket)

        if (!isTryInNewBucket && oldAnswer.tries >= 2) {
          Failure(new Exception("More than 2 tries already..."))
        } else {
          val newBucket = Math.min(if (isCorrect) oldAnswer.bucket + 1 else oldAnswer.bucket, maxBucketId)

          val newTries: Int = if (isTryInNewBucket) {
            0
          } else if (isCorrect) {
            oldAnswer.tries
          } else {
            oldAnswer.tries + 1
          }

          Success((
            correctionResult.copy(newTriesCount = newTries, maybeSampleSolution = None),
            oldAnswer.copy(bucket = newBucket, dateAnswered = today, correct = isCorrect, tries = newTries)
          ))
        }
    }
  }

}
