package model


final case class CollectionBasics(collectionId: Int, courseId: Int, frontLanguageId: Int, backLanguageId: Int, name: String)


final case class FlashcardToAnswer(
  cardId: Int,
  collId: Int,
  courseId: Int,
  cardType: CardType,
  front: String,
  frontHint: Option[String],
  back: String,
  backHint: Option[String],
  frontToBack: Boolean,
  blanksAnswerFragments: Seq[BlanksAnswerFragment] = Seq.empty,
  choiceAnswers: Seq[ChoiceAnswer] = Seq.empty,

  currentTries: Int = 0,
  currentBucket: Option[Int]
)
