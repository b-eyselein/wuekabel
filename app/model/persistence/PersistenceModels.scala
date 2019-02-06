package model.persistence

import model._

object PersistenceModels {

  def flashcardToDbFlashcard(fc: Flashcard): DBCompleteFlashcard = {

    val maybeMeaning = fc match {
      case tfc: TextFlashcard => Some(tfc.meaning)
      case wfc: WordFlashcard => Some(wfc.meaning)
      case _                  => None
    }

    val dbChoiceAnswers = fc match {
      case cfc: ChoiceFlashcard => cfc.choiceAnswers
      case _                    => Seq[ChoiceAnswer]()
    }

    val dbBlanksAnswers = fc match {
      case bfc: BlanksFlashcard => bfc.answers
      case _                    => Seq[BlanksAnswer]()
    }

    DBCompleteFlashcard(
      flashcard = DBFlashcard(fc.cardId, fc.collId, fc.cardType, fc.question, maybeMeaning),
      choiceAnswers = dbChoiceAnswers,
      blanksAnswers = dbBlanksAnswers
    )

  }

  def dbFlashcardToFlashcard(dbfc: DBCompleteFlashcard): Flashcard = dbfc.flashcard.cardType match {
    case CardType.Vocable => WordFlashcard(dbfc.cardId, dbfc.collId, dbfc.question, dbfc.meaning.getOrElse(???))
    case CardType.Text    => TextFlashcard(dbfc.cardId, dbfc.collId, dbfc.question, dbfc.meaning.getOrElse(???))

    case CardType.Blank => BlanksFlashcard(dbfc.cardId, dbfc.collId, dbfc.question, dbfc.blanksAnswers)

    case CardType.SingleChoice | CardType.MultipleChoice =>
      ChoiceFlashcard(dbfc.cardId, dbfc.collId, dbfc.question, dbfc.choiceAnswers)
  }

}


final case class DBFlashcard(cardId: Int, collId: Int, cardType: CardType, question: String, meaning: Option[String])

final case class DBCompleteFlashcard(flashcard: DBFlashcard, choiceAnswers: Seq[ChoiceAnswer], blanksAnswers: Seq[BlanksAnswer]) {

  def cardId: Int = flashcard.cardId

  def collId: Int = flashcard.collId

  def question: String = flashcard.question

  def meaning: Option[String] = flashcard.meaning

}