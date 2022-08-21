package repository

import cats.effect.Blocker
import configuration.ApplicationConfig.DatabaseConfig
import configuration.ApplicationConfig.DatabaseConfig.DbConfigEnv
import doobie.Transactor
import doobie.hikari.HikariTransactor
import zio.blocking.Blocking
import zio.interop.catz._
import zio.{Has, Task, ZIO, ZLayer, ZManaged}

object Database {
  object Connection {
    type TransactorEnv = Has[Transactor[Task]]

    // FIXME Make sure we follow the threading rules from doobie (https://tpolecat.github.io/doobie/docs/14-Managing-Connections.html)
    // Based on zio docs: https://zio.dev/version-1.x/howto/interop/with-cats-effect/#using-zio-with-doobie
    def transactor(
        config: DatabaseConfig
    ): ZManaged[Blocking, Throwable, Transactor[Task]] =
      for {
        rt <- ZIO.runtime[Any].toManaged_
        be <- zio.blocking.blockingExecutor.toManaged_ // our blocking EC
        xa <- HikariTransactor
          .newHikariTransactor[Task](
            config.driver, // driver classname
            config.connectionString, // connect URL
            config.username, // username
            config.password, // password
            rt.platform.executor.asEC, // await connection here
            Blocker.liftExecutionContext(
              be.asEC
            ) // execute JDBC operations here
          )
          .toManagedZIO
      } yield xa

    val live: ZLayer[DbConfigEnv with Blocking, Throwable, TransactorEnv] =
      ZLayer.fromServiceManaged[DatabaseConfig, Blocking, Throwable, Transactor[
        Task
      ]](cfg => transactor(cfg))
  }
}
