import authentication.Authentication
import authentication.Authentication.AuthServiceEnv
import repository.{Database, Repository, TodoRepository, UserCredentialRepository}
import zhttp.http.{Http, HttpApp, Method, Request, Response}
import zhttp.service.Server
import zio._
import zio.blocking._
import configuration.ApplicationConfig._
import configuration.{ApplicationConfig, LoggingConfiguration}
import controller.TodoController
import controller.TodoController.TodoControllerEnv
import error.ErrorHandling.{BusinessException, bodyParser, exceptionHandler}
import pdi.jwt.JwtClaim
import resource.{Todo, UserCredential}
import zhttp.http.Middleware.{debug, status}
import zio.logging._
import zhttp.http._
import zio.magic._

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
    case req @ Method.POST -> !! / "login" => bodyParser[UserCredential, AuthServiceEnv](req, Authentication.createJwt)
  }

  def app(implicit jwt: JwtClaim) = Http.collectZIO[Request] {
    case Method.GET -> !! / "todo" => Authentication.roles("admin", "playdates") !@! TodoController.getAll
    case Method.GET -> !! / "todo" / id => TodoController.getById(id)
    case Method.DELETE -> !! / "todo" / id => TodoController.delete(id)
    case req @ Method.POST -> !! / "todo" => bodyParser[Todo, TodoControllerEnv](req, TodoController.create)
    case req @ Method.PATCH -> !! / "todo" / id => bodyParser[Todo, TodoControllerEnv](req, TodoController.update(id, _))
  }

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] = {
    // Server
    // This uses authentication as a separate httpapp which gives you the jwt after...could also try out
    // the auth Middleware, but I'm not sure the jwtclaim gets passed to the downstream app then??
    val finalApp = login ++ Authentication.authentication(Http.forbidden("None shall pass"), app(_))

    // Useful thing to remember app1 <> app2 means if app1 fails then app2 handles it (might be good for general error catching instead of exceptionHandler at some point)
    val server = Server.start(8080, finalApp.catchAll(exceptionHandler) @@ debug)

    // ZIO 1 way (alright....I can see why zio-magic is nicer)
//    val config = DatabaseConfig.live ++ WebConfig.live ++ Blocking.live
//    val transactor = Database.Connection.live >>> Repository.live
//    val credentialRepo = transactor >>> UserCredentialRepository.live
//    val authService = (credentialRepo ++ WebConfig.live) >>> AuthenticationService.live
//    val todoController = ((transactor >>> TodoRepository.live) ++ authService) >>> TodoController.live
//    val repos = Database.Connection.live >>> Repository.live >>> TodoRepository.live >>> TodoController.live
//    val env = (config >>> repos) ++ todoController ++ authService  ++ zio.console.Console.live ++ LoggingConfiguration.live
//    val envWithDebug = env ++ console.Console.live ++ clock.Clock.live
//
//    server.provideLayer(envWithDebug).exitCode

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
      ApplicationConfig.WebConfig.live,
    )

    magic.exitCode
  }
}