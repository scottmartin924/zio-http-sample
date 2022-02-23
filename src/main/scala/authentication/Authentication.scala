package authentication

import error.ErrorHandling.BusinessException
import pdi.jwt.{Jwt, JwtAlgorithm, JwtClaim}
import repository.UserCredentialRepository
import resource.UserCredential
import zhttp.http.{Http, HttpApp, Middleware, Request, Response, Status}
import cats.implicits._
import configuration.ApplicationConfig.WebConfig
import io.circe.parser._
import io.circe.optics.JsonPath.root
import io.circe.syntax.EncoderOps
import io.circe.{jawn, parser}
import zio.ZIO.debug
import zio.{Has, RIO, Task, ZIO, ZLayer}

import java.time.{Clock, ZoneId}

// NOTE: Just used the jwt library dream11 was using...seemed okay as an experiment?
object AuthenticationService {
  private val AUTH_HEADER = "X-Access-Token"
  private val algo = JwtAlgorithm.HS512


  def authentication[R, E](fail: HttpApp[R, E], success: JwtClaim => HttpApp[R, E]): HttpApp[R with AuthServiceEnv, E] = {
    Http.fromFunctionZIO[Request] { request =>
      val job = for {
        (_, token) <- ZIO.fromOption(request.getHeader(AUTH_HEADER))
        jwt <- AuthenticationService.decode(token.toString)
      } yield jwt
      job.fold(_ => fail, success)
    }.flatten
  }

  type AuthServiceEnv = Has[Service]
  trait AuthenticationService {
    def createJwt(credentials: UserCredential): Task[Response]
    def encode(user: String, roles: List[String]): Task[String]
    def decode(token: String): Task[JwtClaim]
    def getRoles(jwt: JwtClaim): Task[List[String]]
  }

  class Service(authRepo: UserCredentialRepository.Service, config: WebConfig) extends AuthenticationService {
    implicit val clock = java.time.Clock.systemUTC() // Could try to pull this out and use a zio clock, but not a big deal

    def createJwt(credentials: UserCredential): Task[Response] = for {
      user <- authRepo.login(credentials).someOrFail(BusinessException(Status.FORBIDDEN, code = "invalid-credentials", details = s"User credentials invalid"))
      roles <- authRepo.roles(user.userId.get)
    } yield Response.json(s"""{"jwt": "${encode(credentials.username, roles)}"}""")

    def encode(user: String, roles: List[String]): Task[String] = {
      // Looks like this jwt library issuedNow requires an implicit java clock :(. Could maybe override that w/ zio
      val claim = JwtClaim(s"""{"user": "$user", "roles": ${roles.asJson.noSpaces}}""").issuedNow.expiresIn(120)
      Task.succeed(Jwt.encode(claim, config.secret, algo)) // Not sure this really a succeed
    }

    def decode(token: String): Task[JwtClaim] = {
      ZIO.fromOption(Jwt.decode(token, config.secret, Seq(algo)).toOption)
        .orElseFail(new Exception("Unable to decode JWT"))
    }

    def getRoles(jwt: JwtClaim): Task[List[String]] = {
      val _roles = root.roles.each.string
      Task.fromEither(parse(jwt.content).map(_roles.getAll))
    }
  }

  def createJwt(credential: UserCredential): RIO[AuthServiceEnv, Response] = ZIO.accessM(_.get.createJwt(credential))
  def encode(user: String, roles: List[String]): RIO[AuthServiceEnv, String] = ZIO.accessM(_.get.encode(user, roles))
  def decode(token: String): RIO[AuthServiceEnv, JwtClaim] = ZIO.accessM(_.get.decode(token))
  def getRoles(jwt: JwtClaim): RIO[AuthServiceEnv, List[String]] = ZIO.accessM(_.get.getRoles(jwt))

  val live = ZLayer.fromServices[UserCredentialRepository.Service, WebConfig, Service]((repo, webconfig) => new Service(repo, webconfig))
}
