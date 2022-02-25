package resource

import cats.implicits
import doobie.ConnectionIO
import doobie.implicits.toSqlInterpolator
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import repository.DoobieLogging

case class UserCredential(userId: Option[Int] = implicits.none[Int], username: String, password: String)
object UserCredential extends DoobieLogging {
  implicit val decoder: Decoder[UserCredential] = deriveDecoder[UserCredential]
  implicit val encoder: Encoder[UserCredential] = deriveEncoder[UserCredential]

  def login(username: String, password: String): ConnectionIO[Option[UserCredential]] =
    sql"""select user_id, username, password from user_credentials where username = $username and password = $password"""
      .query[UserCredential]
      .option

  def roles(userId: Int): ConnectionIO[List[String]] =
    sql"""select "role" from user_role where user_id = $userId"""
      .query[String]
      .to[List]
}
