package model.persistence

import model._

object PersistenceModels {

  def flashcardToDbFlashcard(fc: Flashcard): DBCompleteFlashcard = DBCompleteFlashcard(
    flashcard = DBFlashcard(fc.cardId, fc.collId, fc.courseId, fc.cardType, fc.question, fc.meaning),
    choiceAnswers = fc.choiceAnswers, blanksAnswers = fc.blanksAnswers
  )

  def dbFlashcardToFlashcard(dbfc: DBCompleteFlashcard): Flashcard = dbfc match {
    case DBCompleteFlashcard(DBFlashcard(cardId, collId, courseId, cardType, question, meaning), choiceAnswers, blanksAnswers) =>
      Flashcard(cardId, collId, courseId, cardType, question, meaning, blanksAnswers, choiceAnswers)
  }

}


final case class DBFlashcard(cardId: Int, collId: Int, courseId: Int, cardType: CardType, question: String, meaning: String)

final case class DBCompleteFlashcard(flashcard: DBFlashcard, choiceAnswers: Seq[ChoiceAnswer], blanksAnswers: Seq[BlanksAnswer])


trait FlashcardToDoData {
  val cardId  : Int
  val collId  : Int
  val courseId: Int
  val username: String

  def cardIdentifier: FlashcardIdentifier = FlashcardIdentifier(cardId, collId, courseId)
}

final case class FlashcardToLearnData(cardId: Int, collId: Int, courseId: Int, username: String) extends FlashcardToDoData

final case class FlashcardToRepeatData(cardId: Int, collId: Int, courseId: Int, username: String) extends FlashcardToDoData
