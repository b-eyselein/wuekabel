package model


final case class CollectionBasics(collectionId: Int, courseId: Int, frontLanguageId: Int, backLanguageId: Int, name: String)


final case class FlashcardToAnswer(
  flashcard: Flashcard,
  frontToBack: Boolean,
  currentTries: Int = 0,
  currentBucket: Option[Int]
)
