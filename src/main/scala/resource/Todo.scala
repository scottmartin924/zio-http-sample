package resource

import cats.implicits.none

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
}
