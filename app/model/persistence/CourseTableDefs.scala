package model.persistence

import model.Consts.{idName, nameName}
import model.Course
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import slick.lifted.ProvenShape

import scala.concurrent.{ExecutionContext, Future}

trait CourseTableDefs extends HasDatabaseConfigProvider[JdbcProfile] {

  protected val dbConfigProvider: DatabaseConfigProvider

  implicit val ec: ExecutionContext

  import profile.api._

  // TableQueries

  protected val coursesTQ: TableQuery[CoursesTable] = TableQuery[CoursesTable]

  // Queries

  def futureAllCourses: Future[Seq[Course]] = db.run(coursesTQ.result)

  def futureCourseById(id: String): Future[Option[Course]] = db.run(coursesTQ.filter(_.id === id).result.headOption)

  def futureInsertCourse(course: Course): Future[Boolean] = db.run(coursesTQ += course).transform(_ == 1, identity)

  // Table definitions

  class CoursesTable(tag: Tag) extends Table[Course](tag, "courses") {

    def id: Rep[String] = column[String](idName, O.PrimaryKey)

    def name: Rep[String] = column[String](nameName)


    override def * : ProvenShape[Course] = (id, name) <> (Course.tupled, Course.unapply)

  }

}

