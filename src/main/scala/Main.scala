import authentication.Authentication
import authentication.Authentication.AuthServiceEnv
import repository.{
  Database,
  Repository,
  TodoRepository,
  UserCredentialRepository
}
import zhttp.http.{Http, HttpApp, Method, Request, Response, _}
import zhttp.service.Server
import zio._
import zio.blocking._
import configuration.ApplicationConfig._
import configuration.{ApplicationConfig, LoggingConfiguration}
import controller.TodoController
import error.ErrorHandling.{BusinessException, bodyParser, exceptionHandler}
import pdi.jwt.JwtClaim
import resource.{Todo, UserCredential}
import zhttp.http.Middleware.{debug, status}
import zio.logging._
import zio.magic._
import Authentication._
import authentication.RoleQuery._
import cats.data.OptionT
import cats.data.EitherT

object Main extends App {

  /* TODO
   - Look into @accessible to avoid the accessor methods...but honestly doesn't seem that great
   - Look into taking care of
      - json serde in middleware (so we can definitely have it incoming as circe json, but is that really very useful?)
      - path parameter type validation (although maybe not a huge deal)
   - Could probably do with a service layer between repo and controller...need to decide how that would look: is the
     repo returning connectionios that the service cna compose to db transactions or repo returns Task and repo's in
     charge of transactions (I think the former...also I think put the sql in companion of object of the resource...sort of like a built-in DAO)
   */

  val login = Http.collectZIO[Request] {
    case Method.GET -> !! / "hello" / name =>
      ZIO.succeed(Response.text(s"hello, $name"))
    case req @ Method.POST -> !! / "login" =>
      log.info(s"Login request") *> bodyParser[UserCredential](
        req
      ) >>= Authentication.createJwt
  }

  // NOTE: Could use #!# as an alias for *> basically or come up with another operator if we need something more specific
  def app(implicit jwt: JwtClaim) = Http.collectZIO[Request] {
    case Method.GET -> !! / "todo" =>
      roles("admin" or "supervisor") *> TodoController.getAll
    case Method.GET -> !! / "todo" / long(id) => TodoController.getById(id)
    case Method.DELETE -> !! / "todo" / long(id) =>
      roles("admin") *> roles("admin" or "") *> TodoController.delete(id)
    case req @ Method.POST -> !! / "todo" =>
      roles("admin" or "supervisor") *> bodyParser[Todo](
        req
      ) >>= TodoController.create
    case req @ Method.PATCH -> !! / "todo" / long(id) =>
      roles("admin") *> bodyParser[Todo](req).flatMap(
        TodoController.update(id, _)
      ) // NOTE: I'm not sure why but >>= w/ the the partial application fails here
  }

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] = {
    // Server
    // This uses authentication as a separate httpapp which gives you the jwt after...could also try out
    // the auth Middleware, but I'm not sure the jwtclaim gets passed to the downstream app then...and _I think_ we need
    // that to do role-specific authorizations since I don't think the middleware will know the matching path yet
    val finalApp = login ++ Authentication.authenticationApp(
      Http.forbidden("None shall pass"),
      app(_)
    )

    // ## Useful thing to remember app1 <> app2 means if app1 fails then app2 handles it (might be good for general error catching instead of exceptionHandler at some point)
    val server =
      Server.start(8080, finalApp.catchAll(exceptionHandler) @@ debug)

    // zio-magic way
    val magic = server.inject(
      DatabaseConfig.live,
      Blocking.live,
      Database.Connection.live,
      Repository.live,
      TodoRepository.live,
      TodoController.live,
      zio.console.Console.live,
      LoggingConfiguration.live,
      clock.Clock.live,
      UserCredentialRepository.live,
      Authentication.live,
      ApplicationConfig.WebConfig.live
    )

    magic.exitCode
  }
}
