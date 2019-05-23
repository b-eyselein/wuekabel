package model.persistence

import model._


object PersistenceModels {

  def collFromDbColl(dbValues: (CollectionBasics, Language, Language)): Collection = dbValues match {
    case (dbColl, frontLang, backLang) => Collection(dbColl.collectionId, dbColl.courseId, frontLang, backLang, dbColl.name)
  }

  def dbCollFromColl(collection: Collection): CollectionBasics = ???


  def flashcardToDbFlashcard(fc: Flashcard): DBCompleteFlashcard = DBCompleteFlashcard(
    flashcard = DBFlashcard(fc.cardId, fc.collId, fc.courseId, fc.cardType, fc.front, fc.frontHint, fc.back, fc.backHint),
    choiceAnswers = fc.choiceAnswers, blanksAnswers = fc.blanksAnswers
  )

}

final case class DBFlashcard(cardId: Int, collId: Int, courseId: Int, cardType: CardType, front: String, frontHint: Option[String], back: String, backHint: Option[String])

final case class DBCompleteFlashcard(flashcard: DBFlashcard, choiceAnswers: Seq[ChoiceAnswer], blanksAnswers: Seq[BlanksAnswerFragment])

final case class FlashcardToAnswerData(cardId: Int, collId: Int, courseId: Int, username: String, frontToBack: Boolean)
