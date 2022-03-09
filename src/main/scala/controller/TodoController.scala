package controller

import authentication.Authentication
import authentication.Authentication.{AuthServiceEnv, AuthenticationService}
import cats.implicits.catsSyntaxOptionId
import error.ErrorHandling.{BusinessException, ErrorResponse}
import io.circe.syntax.EncoderOps
import pdi.jwt.JwtClaim
import repository.Repository
import resource.Todo
import resource.Todo._
import zhttp.http.{Response, Status}
import zio.logging.{Logging, log}
import zio.{Has, RIO, Task, ZIO, ZLayer}

object TodoController {
  // Note Should probably be programming to interfaces better here ehh
  // FIXME For some reason these type aliases were causing compilation errors when used...for now just not going to use them
//  type TodoControllerEnv = Has[TodoController.Service]
//  type TodoControllerRIO = RIO[TodoControllerEnv, Response]

  class Service(repo: Repository[Long, Todo]) {
    // FIXME So much code duplication w/ the entity matches
    // FIXME So much toJson...that happens in API atm too, but would be nice to avoid...I don't think a middleware can handle it, but a separate httpapp could...not sure if it's worth it?
    def getAll: ZIO[Logging, Throwable, Response] = log.info(s"Get all todos") *> repo.getAll.map(todos => Response.json(todos.asJson.noSpaces).setStatus(Status.OK))

    def getById(id: Long) = for {
      _ <- log.info(s"Get todo by id $id")
      entity <- repo.getById(id)
    } yield {
      entity match {
        case None => Response.json(ErrorResponse(code = "not-found", details = s"Todo with id $id was not found".some).asJson.noSpaces).setStatus(Status.NOT_FOUND)
        case Some(todo) => Response.json(todo.asJson.noSpaces).setStatus(Status.OK)
      }
    }

    def create(todo: Todo) = log.info(s"Creating todo $todo") *>
            repo.create(todo).map(todo => Response.json(todo.asJson.noSpaces).setStatus(Status.OK))
    def update(id: Long, todo: Todo) = for {
      _ <- log.info(s"Update todo with id $id too $todo")
      entity <- repo.update(id, todo)
    } yield {
      entity match {
        case None => Response.json(ErrorResponse(code = "not-found", details = s"Todo with id $id was not found".some).asJson.noSpaces).setStatus(Status.NOT_FOUND)
        case Some(todo) => Response.json(todo.asJson.noSpaces).setStatus(Status.OK)
      }
    }
    def delete(id: Long) = for {
      _ <- log.info(s"Delete todo with id $id")
      entity <- repo.delete(id)
    } yield {
      entity match {
        case None => Response.json(ErrorResponse(code = "not-found", details = s"Todo with id $id was not found".some).asJson.noSpaces).setStatus(Status.NOT_FOUND)
        case Some(todo) => Response.json(todo.asJson.noSpaces).setStatus(Status.OK)
      }
    }
  }

  val live = ZLayer.fromService[Repository[Long, Todo], TodoController.Service](repo => new Service(repo))

  def getAll: RIO[Has[TodoController.Service] with Logging, Response] = ZIO.accessM(env => env.get.getAll)
  def getById(id: Long): RIO[Has[TodoController.Service] with Logging, Response] = ZIO.accessM(_.get.getById(id))
  def create(todo: Todo): RIO[Has[TodoController.Service] with Logging, Response] = ZIO.accessM(_.get.create(todo))
  def update(id: Long, todo: Todo): RIO[Has[TodoController.Service] with Logging, Response] = ZIO.accessM(_.get.update(id, todo))
  def delete(id: Long): RIO[Has[TodoController.Service] with Logging, Response] = ZIO.accessM(_.get.delete(id))
}
