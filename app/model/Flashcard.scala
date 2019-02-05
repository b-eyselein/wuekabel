package model

sealed trait Flashcard {

  val cardId  : Int
  val collId  : Int
  val langId  : Int
  val cardType: CardType
  val question: String

  def identifier: FlashcardIdentifier = FlashcardIdentifier(cardId, collId, langId)

}

// Text and Words

final case class WordFlashcard(cardId: Int, collId: Int, langId: Int, question: String, meaning: String) extends Flashcard {

  override val cardType: CardType = CardType.Vocable

}

final case class TextFlashcard(cardId: Int, collId: Int, langId: Int, question: String, meaning: String) extends Flashcard {

  override val cardType: CardType = CardType.Text

}

// Blanks

final case class BlanksAnswer(answerId: Int, cardId: Int, collId: Int, langId: Int, answer: String)

final case class BlanksFlashcard(cardId: Int, collId: Int, langId: Int, question: String, answers: Seq[BlanksAnswer]) extends Flashcard {

  override val cardType: CardType = CardType.Blank

}

// Single and Multiple choice

final case class ChoiceAnswer(id: Int, cardId: Int, collId: Int, langId: Int, answer: String, correctness: Correctness)

final case class ChoiceFlashcard(cardId: Int, collId: Int, langId: Int, question: String, choiceAnswers: Seq[ChoiceAnswer]) extends Flashcard {

  val isMultipleChoice: Boolean = choiceAnswers.count(_.correctness != Correctness.Wrong) >= 2

  override val cardType: CardType = if (isMultipleChoice) CardType.MultipleChoice else CardType.SingleChoice

}