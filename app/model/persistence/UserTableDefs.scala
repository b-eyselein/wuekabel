package model.persistence

import model.Consts._
import model.{Course, User, UserPassword}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import slick.lifted.{ForeignKeyQuery, PrimaryKey, ProvenShape}

import scala.concurrent.{ExecutionContext, Future}

trait UserTableDefs extends HasDatabaseConfigProvider[JdbcProfile] {

  protected val dbConfigProvider: DatabaseConfigProvider

  implicit val ec: ExecutionContext

  import profile.api._

  // TableQueries

  protected val usersTQ: TableQuery[UsersTable] = TableQuery[UsersTable]

  protected val userPasswordsTQ: TableQuery[UserPasswordsTable] = TableQuery[UserPasswordsTable]

  protected val coursesTQ: TableQuery[CoursesTable] = TableQuery[CoursesTable]

  protected val usersInCoursesTQ: TableQuery[UsersInCoursesTable] = TableQuery[UsersInCoursesTable]

  // Queries - User

  def futureUserByUserName(username: String): Future[Option[User]] = db.run(usersTQ.filter(_.username === username).result.headOption)

  def futureInsertUser(user: User): Future[Boolean] = db.run(usersTQ += user).transform(_ == 1, identity)

  // Queries - UserPasswords

  def futurePwHashForUser(username: String): Future[Option[UserPassword]] = db.run(userPasswordsTQ.filter(_.username === username).result.headOption)

  def futureSavePwHash(userPassword: UserPassword): Future[Boolean] = db.run(userPasswordsTQ += userPassword).transform(_ == 1, identity)

  // Queries - Courses

  def futureAllCourses: Future[Seq[Course]] = db.run(coursesTQ.result)

  def futureCourseById(id: String): Future[Option[Course]] = db.run(coursesTQ.filter(_.id === id).result.headOption)

  def futureInsertCourse(course: Course): Future[Boolean] = db.run(coursesTQ += course).transform(_ == 1, identity)

  // Queries - UsersInCourses

  def futureUserInCourse(user: User, course: Course): Future[Boolean] =
    db.run(usersInCoursesTQ.filter {
      uic => uic.username === user.username && uic.courseId === course.id
    }.result.map(_.nonEmpty))

  def futureAddUserToCourse(user: User, course: Course): Future[Boolean] =
    db.run(usersInCoursesTQ += (user.username, course.id)).transform(_ == 1, identity)

  // Table definitions

  class UsersTable(tag: Tag) extends Table[User](tag, "users") {

    def username: Rep[String] = column[String](usernameName, O.PrimaryKey)

    def isAdmin: Rep[Boolean] = column[Boolean]("is_admin")

    override def * : ProvenShape[User] = (username, isAdmin) <> (User.tupled, User.unapply)

  }

  class UserPasswordsTable(tag: Tag) extends Table[UserPassword](tag, "user_passwords") {

    def username: Rep[String] = column[String](usernameName, O.PrimaryKey)

    def pwHash: Rep[String] = column[String]("password_hash")


    override def * : ProvenShape[UserPassword] = (username, pwHash) <> (UserPassword.tupled, UserPassword.unapply)

  }

  class CoursesTable(tag: Tag) extends Table[Course](tag, "courses") {

    def id: Rep[String] = column[String](idName, O.PrimaryKey)

    def name: Rep[String] = column[String](nameName)


    override def * : ProvenShape[Course] = (id, name) <> (Course.tupled, Course.unapply)

  }

  class UsersInCoursesTable(tag: Tag) extends Table[(String, String)](tag, "users_in_courses") {

    def username: Rep[String] = column[String](usernameName)

    def courseId: Rep[String] = column[String]("course_id")


    def pk: PrimaryKey = primaryKey("pk", (username, courseId))

    def userFk: ForeignKeyQuery[UsersTable, User] = foreignKey("uic_user_fk", username, usersTQ)(_.username)

    def courseFk: ForeignKeyQuery[CoursesTable, Course] = foreignKey("uic_course_fk", courseId, coursesTQ)(_.id)


    def * : ProvenShape[(String, String)] = (username, courseId)

  }

}
