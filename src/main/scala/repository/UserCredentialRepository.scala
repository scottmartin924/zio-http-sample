package repository

import doobie.ConnectionIO
import doobie.implicits.toSqlInterpolator
import repository.Repository.ExecutorEnv
import resource.UserCredential
import zio.{Has, RIO, Task, ZIO, ZLayer}

object UserCredentialRepository extends DoobieLogging {

  // FIXME Program to traits
  type UserCredentialEnv = Has[Service]

  class Service(db: Repository.Executor) {
    def login(credentials: UserCredential): Task[Option[UserCredential]] = db.execute(Sql.login(credentials.username, credentials.password))
    def roles(userId: Int): Task[List[String]] = db.execute(Sql.roles(userId))
  }

  def login(credentials: UserCredential): RIO[UserCredentialEnv, Option[UserCredential]] = ZIO.accessM(_.get.login(credentials))
  def roles(userId: Int): RIO[UserCredentialEnv, List[String]] = ZIO.accessM(_.get.roles(userId))

  val live: ZLayer[ExecutorEnv, Throwable, UserCredentialEnv] = ZLayer.fromService[Repository.Executor, Service](db => new UserCredentialRepository.Service(db))

  object Sql {
    def login(username: String, password: String): ConnectionIO[Option[UserCredential]] =
      sql"""select user_id, username, password from user_credentials where username = $username and password = $password"""
        .query[UserCredential]
        .option

    def roles(userId: Int): ConnectionIO[List[String]] =
      sql"""select "role" from user_role where user_id = $userId"""
        .query[String]
        .to[List]
  }
}
