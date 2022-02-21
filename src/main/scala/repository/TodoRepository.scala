package repository

import cats.implicits.catsSyntaxOptionId
import doobie.ConnectionIO
import doobie.implicits.toSqlInterpolator
import resource.Todo
import zio.{Has, RIO, ZIO, ZLayer}
import doobie.postgres.implicits._
import repository.Repository.ExecutorEnv

import java.time.Instant

object TodoRepository extends DoobieLogging {
  type TodoRepoEnv = Has[Repository[Long, Todo]]

  class Service(db: Repository.Executor) extends Repository[Long, Todo] {
    override def getAll = db.execute(Sql.getAll)
    override def getById(id: Long) = db.execute(Sql.getById(id))
    override def create(value: Todo) = db.execute(Sql.create(value))
    override def update(id: Long, value: Todo) = db.execute(Sql.update(id, value))
    override def delete(key: Long) = db.execute(Sql.delete(key))
  }

  // TODO I think we should probably have the service and the repo (the SQL) separate...exposing connectionio allows creating
  // transactions...maybe only in the service (or maybe even in the controller) we actually run the connection io
  def getAll: RIO[TodoRepoEnv, List[Todo]] = ZIO.accessM(_.get.getAll)
  def getById(id: Long): RIO[TodoRepoEnv, Option[Todo]] = ZIO.accessM(_.get.getById(id))
  def create(todo: Todo): RIO[TodoRepoEnv, Todo] = ZIO.accessM(_.get.create(todo))
  def update(id: Long, todo: Todo): RIO[TodoRepoEnv, Option[Todo]] = ZIO.accessM(_.get.update(id, todo))
  def delete(id: Long): RIO[TodoRepoEnv, Option[Todo]] = ZIO.accessM(_.get.delete(id))

  // TODO I really hate the type aliases not being in one place
  val live: ZLayer[ExecutorEnv, Throwable, TodoRepoEnv] = ZLayer.fromService[Repository.Executor, Repository[Long, Todo]](db => new Service(db))

  // TODO Consider if this belongs in the resource class itself? I think they belong in the companion of the resource
  // Justifications: That's where it's clear what the class looks like, that ties the entity to the SQL which makes sense, that exposes connectionIO
  // via the resource, allows the import of doobie.implicits._ to be in the same spot where the tricky type is
  // So I think that becomes the repo (or something like it) and then the service stays here. Or happy to call it a in there
  object Sql {
    def getAll: ConnectionIO[List[Todo]] = {
      sql"""select id, entry, created_at, updated_at from todo"""
        .query[Todo]
        .to[List]
    }

    def getById(id: Long): ConnectionIO[Option[Todo]] = {
      sql"select id, entry, created_at, updated_at from todo where id = $id"
        .query[Todo]
        .option
    }

    def create(todo: Todo): ConnectionIO[Todo] = {
      sql"""insert into todo(entry) values (${todo.entry})"""
        .update
        .withUniqueGeneratedKeys[(Long, Instant, Instant)]("id", "created_at", "updated_at")
        .map { case (id, createdAt, updatedAt) => todo.copy(id = id.some, createdAt = createdAt.some, updatedAt = updatedAt.some) }
    }

    // FIXME Probably a better way to handle this w/ sending back data...I want to delete/update, but also get the entity back...withGeneratedKeysMaybe?
    def update(id: Long, todo: Todo): ConnectionIO[Option[Todo]] =
      for {
      entity <- getById(id)
      updated <- sql"""update todo set entry = ${todo.entry}, updated_at = now() where id = $id""".update.run
    } yield entity.map(original => original.copy(entry = todo.entry))

    def delete(id: Long): ConnectionIO[Option[Todo]] = for {
      entity <- getById(id)
      _ <- sql"delete from todo where id = $id".update.run
    } yield entity
  }
}
