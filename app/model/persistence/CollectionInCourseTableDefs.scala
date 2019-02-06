package model.persistence


import model.{Collection, CollectionInCourse, Course}
import play.api.db.slick.HasDatabaseConfigProvider
import slick.jdbc.JdbcProfile
import slick.lifted.{ForeignKeyQuery, PrimaryKey, ProvenShape}

import scala.concurrent.Future

trait CollectionInCourseTableDefs extends HasDatabaseConfigProvider[JdbcProfile] with CollectionTableDefs with CourseTableDefs {

  import profile.api._

  // TableQueries

  protected val collectionInCoursesTQ: TableQuery[CollectionsInCoursesTable] = TableQuery[CollectionsInCoursesTable]

  // Queries

  def futureCollectionsForCourse(course: Course): Future[Seq[Collection]] = db.run(
    collectionInCoursesTQ
      .filter(_.courseId === course.id)
      .join(collectionsTQ).on((cic, c) => cic.collId === c.id)
      .map(_._2)
      .result
  )

  def futureCollectionsAndCourseImportState(course: Course): Future[Seq[(Collection, Boolean)]] =
    futureAllCollections flatMap { allCollections =>

      Future.sequence(allCollections.map { collection =>
        db.run(collectionInCoursesTQ.filter(_.collId === collection.id).result.headOption.map(_.isDefined)) map {
          isImportedInCourse => (collection, isImportedInCourse)
        }
      })
    }

  def allocateCollectionToCourse(course: Course, collection: Collection): Future[Boolean] =
    db.run(collectionInCoursesTQ += CollectionInCourse(collection.id, course.id)).transform(_ == 1, identity)

  def deallocateCollectionFromCourse(course: Course, collection: Collection): Future[Boolean] =
    db.run(collectionInCoursesTQ.filter(cic => cic.courseId === course.id && cic.collId === collection.id).delete).transform(_ >= 1, identity)

  // Table Defs

  class CollectionsInCoursesTable(tag: Tag) extends Table[CollectionInCourse](tag, "collections_in_courses") {

    def collId: Rep[Int] = column[Int]("coll_id")

    def courseId: Rep[String] = column[String]("course_id")


    def pk: PrimaryKey = primaryKey("cic_pk", (collId, courseId))

    def collFk: ForeignKeyQuery[CollectionsTable, Collection] = foreignKey("cic_coll_fk", collId, collectionsTQ)(_.id)

    def courseFk: ForeignKeyQuery[CoursesTable, Course] = foreignKey("cic_course_fk", courseId, coursesTQ)(_.id)


    def * : ProvenShape[CollectionInCourse] = (collId, courseId) <> (CollectionInCourse.tupled, CollectionInCourse.unapply)

  }

}
