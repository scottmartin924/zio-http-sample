package resource

import cats.implicits.{catsSyntaxOptionId, none}
import doobie.ConnectionIO
import doobie.implicits.toSqlInterpolator

import java.time.Instant
import doobie.postgres.implicits._
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

// TODO Consider if we want separate dtos and entities (e.g. so id isn't option here)? Honestly doesn't seem worth it
// to split for this example, but I'm curious what's "best practice"
// TODO Really we should have a TodoId class probably so not just hardcoding Long

case class Todo(id: Option[Long] = none[Long], entry: String, createdAt: Option[Instant] = none[Instant], updatedAt: Option[Instant] = none[Instant])
object Todo {
  implicit val decoder: Decoder[Todo] = deriveDecoder[Todo]
  implicit val encoder: Encoder[Todo] = deriveEncoder[Todo]

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
