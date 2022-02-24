package repository

import doobie.ConnectionIO
import doobie.implicits.toSqlInterpolator
import repository.Repository.ExecutorEnv
import resource.UserCredential
import zio.{Has, RIO, Task, ZIO, ZLayer}

object UserCredentialRepository extends DoobieLogging {
  type UserCredentialEnv = Has[Service]

  // FIXME Naming here sucks
  trait Service {
    def login(credentials: UserCredential): Task[Option[UserCredential]]
    def roles(userId: Int): Task[List[String]]
  }

  class ServiceImpl(db: Repository.Executor) extends Service {
    def login(credentials: UserCredential): Task[Option[UserCredential]] = db.execute(UserCredential.login(credentials.username, credentials.password))
    def roles(userId: Int): Task[List[String]] = db.execute(UserCredential.roles(userId))
  }

  def login(credentials: UserCredential): RIO[UserCredentialEnv, Option[UserCredential]] = ZIO.accessM(_.get.login(credentials))
  def roles(userId: Int): RIO[UserCredentialEnv, List[String]] = ZIO.accessM(_.get.roles(userId))

  val live: ZLayer[ExecutorEnv, Throwable, UserCredentialEnv] = ZLayer.fromService[Repository.Executor, Service](db => new UserCredentialRepository.ServiceImpl(db))
}
