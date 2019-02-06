package model

sealed trait Flashcard {

  val cardId  : Int
  val collId  : Int
  val cardType: CardType
  val question: String

  def identifier: FlashcardIdentifier = FlashcardIdentifier(cardId, collId)

}

// Text and Words

final case class WordFlashcard(cardId: Int, collId: Int, question: String, meaning: String) extends Flashcard {

  override val cardType: CardType = CardType.Vocable

}

final case class TextFlashcard(cardId: Int, collId: Int, question: String, meaning: String) extends Flashcard {

  override val cardType: CardType = CardType.Text

}

sealed trait CardAnswer {
  val answerId: Int
  val cardId  : Int
  val collId  : Int
  val answer  : String
}

// Blanks

final case class BlanksAnswer(answerId: Int, cardId: Int, collId: Int, answer: String) extends CardAnswer

final case class BlanksFlashcard(cardId: Int, collId: Int, question: String, answers: Seq[BlanksAnswer]) extends Flashcard {

  override val cardType: CardType = CardType.Blank

}

// Single and Multiple choice

final case class ChoiceAnswer(answerId: Int, cardId: Int, collId: Int, answer: String, correctness: Correctness) extends CardAnswer

final case class ChoiceFlashcard(cardId: Int, collId: Int, question: String, choiceAnswers: Seq[ChoiceAnswer]) extends Flashcard {

  val isMultipleChoice: Boolean = choiceAnswers.count(_.correctness != Correctness.Wrong) >= 2

  override val cardType: CardType = if (isMultipleChoice) CardType.MultipleChoice else CardType.SingleChoice

}