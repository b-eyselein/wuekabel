package model.persistence

import model.Consts.usernameName
import model.{User, UserPassword}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import slick.lifted.ProvenShape

import scala.concurrent.{ExecutionContext, Future}

trait UserTableDefs extends HasDatabaseConfigProvider[JdbcProfile] {

  protected val dbConfigProvider: DatabaseConfigProvider

  implicit val ec: ExecutionContext

  import profile.api._

  // TableQueries

  protected val usersTQ: TableQuery[UsersTable] = TableQuery[UsersTable]

  protected val userPasswordsTQ: TableQuery[UserPasswordsTable] = TableQuery[UserPasswordsTable]

  // Queries

  def futureUserByUserName(username: String): Future[Option[User]] = db.run(usersTQ.filter(_.username === username).result.headOption)

  def futurePwHashForUser(username: String): Future[Option[UserPassword]] = db.run(userPasswordsTQ.filter(_.username === username).result.headOption)

  def futureSaveUser(user: User): Future[Boolean] = db.run(usersTQ += user).transform(_ == 1, identity)

  def futureSavePwHash(userPassword: UserPassword): Future[Boolean] = db.run(userPasswordsTQ += userPassword).transform(_ == 1, identity)

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

}
