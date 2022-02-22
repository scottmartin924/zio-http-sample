package resource

import cats.implicits
import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, JsonDecoder, JsonEncoder}

case class UserCredential(userId: Option[Int] = implicits.none[Int], username: String, password: String)
object UserCredential {
  implicit val decoder: JsonDecoder[UserCredential] = DeriveJsonDecoder.gen[UserCredential]
  implicit val encoder: JsonEncoder[UserCredential] = DeriveJsonEncoder.gen[UserCredential]
}
