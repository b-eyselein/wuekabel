package model.persistence

import model.Consts.usernameName
import model.{Course, User, UserInCourse}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import slick.lifted.{ForeignKeyQuery, PrimaryKey, ProvenShape}

import scala.concurrent.{ExecutionContext, Future}

trait UserInCourseTableDefs extends HasDatabaseConfigProvider[JdbcProfile] with UserTableDefs with CourseTableDefs {

  protected val dbConfigProvider: DatabaseConfigProvider

  implicit val ec: ExecutionContext

  import profile.api._

  // TableQueries

  protected val usersInCoursesTQ: TableQuery[UsersInCoursesTable] = TableQuery[UsersInCoursesTable]

  // Queries

  def futureUserInCourse(user: User, course: Course): Future[Boolean] =
    db.run(usersInCoursesTQ.filter {
      uic => uic.username === user.username && uic.courseId === course.id
    }.result.map(_.nonEmpty))

  def futureAddUserToCourse(user: User, course: Course): Future[Boolean] =
    db.run(usersInCoursesTQ += UserInCourse(user.username, course.id)).transform(_ == 1, identity)

  def futureCoursesForUser(user: User): Future[Seq[Course]] = db.run(
    usersInCoursesTQ
      .filter(_.username === user.username)
      .join(coursesTQ).on((uic, c) => uic.courseId === c.id)
      .map(_._2)
      .result
  )

  // Table definitions

  class UsersInCoursesTable(tag: Tag) extends Table[UserInCourse](tag, "users_in_courses") {

    def username: Rep[String] = column[String](usernameName)

    def courseId: Rep[String] = column[String]("course_id")


    def pk: PrimaryKey = primaryKey("pk", (username, courseId))

    def userFk: ForeignKeyQuery[UsersTable, User] = foreignKey("uic_user_fk", username, usersTQ)(_.username)

    def courseFk: ForeignKeyQuery[CoursesTable, Course] = foreignKey("uic_course_fk", courseId, coursesTQ)(_.id)


    def * : ProvenShape[UserInCourse] = (username, courseId) <> (UserInCourse.tupled, UserInCourse.unapply)

  }

}
