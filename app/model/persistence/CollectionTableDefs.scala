package model.persistence

import model.Collection
import model.Consts.{idName, nameName}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import slick.lifted.ProvenShape

import scala.concurrent.{ExecutionContext, Future}

trait CollectionTableDefs extends HasDatabaseConfigProvider[JdbcProfile] {

  protected val dbConfigProvider: DatabaseConfigProvider

  implicit val ec: ExecutionContext

  import profile.api._

  // Queries - Collection

  def futureAllCollections: Future[Seq[Collection]] = db.run(collectionsTQ.result)

  def futureCollectionById(collId: Int): Future[Option[Collection]] =
    db.run(collectionsTQ.filter(_.id === collId).result.headOption)

  def futureInsertCollection(collection: Collection): Future[Int] = {
    val query = collectionsTQ returning collectionsTQ.map(_.id) into ((coll, newId) => coll.copy(id = newId))

    db.run(query += collection).map(_.id)
  }


  // TableQueries

  protected val collectionsTQ: TableQuery[CollectionsTable] = TableQuery[CollectionsTable]

  // Table Defs

  class CollectionsTable(tag: Tag) extends Table[Collection](tag, "collections") {

    def id: Rep[Int] = column[Int](idName, O.PrimaryKey, O.AutoInc)

    def name: Rep[String] = column[String](nameName)


    override def * : ProvenShape[Collection] = (id, name) <> (Collection.tupled, Collection.unapply)

  }

}
