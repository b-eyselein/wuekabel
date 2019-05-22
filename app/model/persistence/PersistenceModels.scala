package model.persistence

import model._

object PersistenceModels {

  def flashcardToDbFlashcard(fc: Flashcard): DBCompleteFlashcard = DBCompleteFlashcard(
    flashcard = DBFlashcard(fc.cardId, fc.collId, fc.courseId, fc.cardType, fc.question, fc.questionHint, fc.meaning, fc.meaningHint),
    choiceAnswers = fc.choiceAnswers, blanksAnswers = fc.blanksAnswers
  )

  def dbFlashcardToFlashcard(dbfc: DBCompleteFlashcard): Flashcard = dbfc match {
    case DBCompleteFlashcard(DBFlashcard(cardId, collId, courseId, cardType, question, questionHint, meaning, meaningHint), choiceAnswers, blanksAnswers) =>
      Flashcard(cardId, collId, courseId, cardType, question, questionHint, meaning, meaningHint, blanksAnswers, choiceAnswers)
  }

}


final case class DBFlashcard(cardId: Int, collId: Int, courseId: Int, cardType: CardType, question: String, questionHint: Option[String], meaning: String, meaningHint: Option[String])

final case class DBCompleteFlashcard(flashcard: DBFlashcard, choiceAnswers: Seq[ChoiceAnswer], blanksAnswers: Seq[BlanksAnswerFragment])

final case class FlashcardToAnswerData(cardId: Int, collId: Int, courseId: Int, username: String, leftToRight: Boolean) {

  def cardIdentifier: FlashcardIdentifier = FlashcardIdentifier(cardId, collId, courseId)

}
