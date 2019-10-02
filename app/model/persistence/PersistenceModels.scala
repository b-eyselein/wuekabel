package model.persistence

import model._

object PersistenceModels {

  def collFromDbColl(dbValues: (CollectionBasics, Language, Language)): Collection = dbValues match {
    case (dbColl, frontLang, backLang) => Collection(dbColl.collectionId, dbColl.courseId, frontLang, backLang, dbColl.name)
  }

}

final case class FlashcardToAnswerData(cardId: Int, collId: Int, courseId: Int, username: String, frontToBack: Boolean)
