package resource

import cats.implicits
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

case class UserCredential(userId: Option[Int] = implicits.none[Int], username: String, password: String)
object UserCredential {
  implicit val decoder: Decoder[UserCredential] = deriveDecoder[UserCredential]
  implicit val encoder: Encoder[UserCredential] = deriveEncoder[UserCredential]
}
