package authentication

import error.ErrorHandling.BusinessException
import pdi.jwt.{Jwt, JwtAlgorithm, JwtClaim}
import repository.UserCredentialRepository
import resource.UserCredential
import zhttp.http.{Http, HttpApp, Middleware, Request, Response, Status}
import cats.implicits._
import io.circe.optics.JsonPath.root
import io.circe.{jawn, parser}
import zio.{Has, RIO, Task, ZIO, ZLayer}
import zio.json._

import java.time.Clock

// NOTE: Just used the jwt library dream11 was using...seemed okay?
// FIXME This has a bunch of utlitity type methods...do something smarter
object AuthenticationService {
  private val AUTH_HEADER = "X-Access-Token"

  // FIXME Put secrete in config (for now in plaintext)
  private val SECRET = "topSecret"
  private val algo = JwtAlgorithm.HS512

  implicit val clock: Clock = Clock.systemUTC

  def encode(user: String, roles: List[String]): String = {
    // FIXME Use zio clock if possible
    val claim = JwtClaim(s"""{"user": "$user", "roles": ${roles.toJson}}""").issuedNow.expiresIn(120)
    Jwt.encode(claim, SECRET, algo)
  }

  def decode(token: String): Option[JwtClaim] = {
    Jwt.decode(token, SECRET, Seq(algo)).toOption
  }

  // FIXME This shouldn't be an either....just do a zio.fail if fails...caller won't know what to do
//  def getRoles(jwt: JwtClaim): Either[String, List[String]] = {
//    import io.circe.parser._
//    parse(jwt.content).map(root.roles.each.string.getAll).leftMap(_.message)
//  }

  def getRoles(jwt: JwtClaim): List[String] = {
    import io.circe.parser._
    parse(jwt.content).map(root.roles.each.string.getAll).getOrElse(List.empty[String]) // FIXME Hack...really should zio fail if fails to parse
  }

  def authentication[R, E](fail: HttpApp[R, E], success: JwtClaim => HttpApp[R, E]): HttpApp[R, E] = {
    Http.fromFunction[Request] { request =>
      request.getHeader(AUTH_HEADER)
        .flatMap { case (_, token) => decode(token.toString) }
        .fold(fail)(success)
    }.flatten
  }

  type AuthServiceEnv = Has[Service]
  class Service(authRepo: UserCredentialRepository.Service) {

    def createJwt(credentials: UserCredential): Task[Response] = for {
      user <- authRepo.login(credentials).someOrFail(BusinessException(Status.FORBIDDEN, code = "invalid-credentials", details = s"User credentials invalid"))
      roles <- authRepo.roles(user.userId.get)
    } yield Response.json(s"""{"jwt": "${encode(credentials.username, roles)}"}""")
  }

  def createJwt(credential: UserCredential): RIO[AuthServiceEnv, Response] = ZIO.accessM(_.get.createJwt(credential))

  val live = ZLayer.fromService[UserCredentialRepository.Service, Service](repo => new Service(repo))
}
