package model.persistence

import model.Consts._
import model._
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import slick.lifted.ProvenShape

import scala.concurrent.{ExecutionContext, Future}


trait LanguageTableDefs extends HasDatabaseConfigProvider[JdbcProfile] {

  protected val dbConfigProvider: DatabaseConfigProvider

  implicit val ec: ExecutionContext

  import profile.api._

  // TableQueries

  protected val languagesTQ: TableQuery[LanguagesTable] = TableQuery[LanguagesTable]


  // Queries - Language

  def futureLanguageById(langId: Int): Future[Option[Language]] = db.run(languagesTQ.filter(_.id === langId).result.headOption)

  def futureAllLanguages: Future[Seq[Language]] = db.run(languagesTQ.result)

//  def futureCollectionsForLanguage(language: Language): Future[Seq[Collection]] =
//    db.run(collectionsTQ.filter(_.langId === language.id).result)

  def futureInsertLanguage(language: Language): Future[Int] = {
    val query = languagesTQ returning languagesTQ.map(_.id) into ((lang, newId) => lang.copy(id = newId))

    db.run(query += language).map(_.id)
  }

  // Queries - Flashcard


  // Column types

//  protected implicit val cardTypeColumnType: BaseColumnType[CardType] =
//    MappedColumnType.base[CardType, String](_.entryName, CardType.withNameInsensitive)

//  protected implicit val correctnessColumnType: BaseColumnType[Correctness] =
//    MappedColumnType.base[Correctness, String](_.entryName, Correctness.withNameInsensitive)

  // Table definitions

  class LanguagesTable(tag: Tag) extends Table[Language](tag, "languages") {

    def id: Rep[Int] = column[Int](idName, O.PrimaryKey, O.AutoInc)

    def name: Rep[String] = column[String](nameName)


    override def * : ProvenShape[Language] = (id, name) <> (Language.tupled, Language.unapply)

  }



}
