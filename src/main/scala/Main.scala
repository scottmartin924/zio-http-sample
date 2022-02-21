import cats.implicits.catsSyntaxOptionId
import configuration.ApplicationConfig.WebConfig.WebConfigEnv
import repository.{Database, Repository, TodoRepository}
import zhttp.http.{Http, HttpApp, Method, Request, Response}
import zhttp.service.Server
import zio._
import zio.blocking._
import configuration.ApplicationConfig._
import configuration.LoggingConfiguration
import controller.TodoController
import controller.TodoController.TodoControllerEnv
import error.ErrorHandling.{ErrorResponse, bodyParser, exceptionHandler, unhandledError}
import resource.Todo
import shapeless.syntax.inject.InjectSyntax
import zhttp.http.Middleware.{debug, status}
import zio.logging._
import zhttp.http._
import zio.json._
import zio.magic._

object Main extends App {

  /* TODO
   - try to implement authentication (I think this will be hard and annoying...use Middleware.auth)
   */

  val app = Http.collectZIO[Request] {
    case Method.GET -> !! / "todo" => TodoController.getAll
    case Method.GET -> !! / "todo" / id => TodoController.getById(id).catchAllDefect(defect => Task.succeed(Response.text("oops")))
    case Method.DELETE -> !! / "todo" / id => TodoController.delete(id)
    case req @ Method.POST -> !! / "todo" => bodyParser[Todo, TodoControllerEnv](req, TodoController.create)
    case req @ Method.PATCH -> !! / "todo" / id => bodyParser[Todo, TodoControllerEnv](req, TodoController.update(id, _))
  }

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] = {
    // Server
    // FIXME Get port from web config
    val server = Server.start(8080, app.catchAll(exceptionHandler) @@ debug)

    // ZIO 1 way
//    val config = DatabaseConfig.live ++ WebConfig.live ++ Blocking.live
//    val repos = Database.Connection.live >>> Repository.live >>> TodoRepository.live >>> TodoController.live
//    val env = (config >>> repos) ++ zio.console.Console.live ++ LoggingConfiguration.live
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
      clock.Clock.live
    )

    magic.exitCode
  }
}