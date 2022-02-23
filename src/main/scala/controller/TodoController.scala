package controller

import authentication.AuthenticationService
import authentication.AuthenticationService.AuthServiceEnv
import cats.implicits.catsSyntaxOptionId
import error.ErrorHandling.{BusinessException, ErrorResponse}
import io.circe.syntax.EncoderOps
import pdi.jwt.JwtClaim
import repository.Repository
import resource.Todo
import resource.Todo._
import zhttp.http.{Response, Status}
import zio.{Has, RIO, Task, ZIO, ZLayer}

object TodoController {
  // FIXME Should probably be programming to interfaces better here (e.g. have a trait for what the controller should have, etc)
  type TodoControllerEnv = Has[TodoController.Service]
  type TodoControllerRIO = RIO[TodoControllerEnv, Response]

  class Service(repo: Repository[Long, Todo]) {
    // FIXME Not sure if this should be here...really I don't think it should have to be anywhere...for some reason no catchAllDefect? Could also just wrap things in a try and do fromTry
    private def toLongOrFail(long: String): ZIO[Any, BusinessException, Long] = ZIO.fromOption(long.toLongOption)
      .orElseFail(BusinessException(Status.BAD_REQUEST, code = "not-found", details = s"Failed to parse $long to Long"))

    // FIXME So much code duplication w/ the entity matches
    // FIXME So much toJson...that happens in API atm too, but would be nice to avoid...I don't think a middleware can handle it, but a separate httpapp could...not sure if it's worth it?
    def getAll(jwt: JwtClaim) = {
      // FIXMe BAD IF-ELSE!!
      println(AuthenticationService.getRoles(jwt))
      if (AuthenticationService.getRoles(jwt).contains("admin")) repo.getAll.map(todos => Response.json(todos.asJson.noSpaces).setStatus(Status.OK))
      else Task.fail(BusinessException(Status.FORBIDDEN, code = "insufficient-permissions", "Must have admin permissions to view all todos"))
    }
    def getById(id: String) = for {
      longId <- toLongOrFail(id)
      entity <- repo.getById(longId)
    } yield {
      entity match {
        case None => Response.json(ErrorResponse(code = "not-found", details = s"Todo with id $longId was not found".some).asJson.noSpaces).setStatus(Status.NOT_FOUND)
        case Some(todo) => Response.json(todo.asJson.noSpaces).setStatus(Status.OK)
      }
    }

    def create(todo: Todo) = repo.create(todo).map(todo => Response.json(todo.asJson.noSpaces).setStatus(Status.OK))
    def update(id: String, todo: Todo) = for {
      longId <- toLongOrFail(id)
      entity <- repo.update(longId, todo)
    } yield {
      entity match {
        case None => Response.json(ErrorResponse(code = "not-found", details = s"Todo with id $longId was not found".some).asJson.noSpaces).setStatus(Status.NOT_FOUND)
        case Some(todo) => Response.json(todo.asJson.noSpaces).setStatus(Status.OK)
      }
    }
    def delete(id: String) = for {
      longId <- toLongOrFail(id)
      entity <- repo.delete(longId)
    } yield {
      entity match {
        case None => Response.json(ErrorResponse(code = "not-found", details = s"Todo with id $longId was not found".some).asJson.noSpaces).setStatus(Status.NOT_FOUND)
        case Some(todo) => Response.json(todo.asJson.noSpaces).setStatus(Status.OK)
      }
    }
  }

  val live = ZLayer.fromService[Repository[Long, Todo], TodoController.Service](repo => new Service(repo))

  def getAll(jwt: JwtClaim): TodoControllerRIO = ZIO.accessM(_.get.getAll(jwt))
  def getById(id: String): TodoControllerRIO = ZIO.accessM(_.get.getById(id))
  def create(todo: Todo): TodoControllerRIO = ZIO.accessM(_.get.create(todo))
  def update(id: String, todo: Todo): TodoControllerRIO = ZIO.accessM(_.get.update(id, todo))
  def delete(id: String): TodoControllerRIO = ZIO.accessM(_.get.delete(id))
}
