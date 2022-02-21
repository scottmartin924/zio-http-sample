package controller

import cats.implicits.catsSyntaxOptionId
import error.ErrorHandling.{BusinessException, ErrorResponse}
import repository.Repository
import resource.Todo
import resource.Todo._
import zhttp.http.{Response, Status}
import zio.{Has, RIO, ZIO, ZLayer}
import zio.json.EncoderOps

object TodoController {
  // FIXME Should probably be programming to interfaces better here (e.g. have a trait for what the controller should have, etc)
  type TodoControllerEnv = Has[TodoController.Service]
  type TodoControllerRIO = RIO[TodoControllerEnv, Response]

  class Service(repo: Repository[Long, Todo]) {
    // FIXME Not sure if this should be here...really I don't think it should have to be anywhere...for some reason no catchAllDefect?
    private def toLongOrFail(long: String): ZIO[Any, BusinessException, Long] = ZIO.fromOption(long.toLongOption)
      .orElseFail(BusinessException(Status.BAD_REQUEST, code = "not-found", details = s"Failed to parse $long to Long"))

    // FIXME So much code duplication w/ the entity matches
    // FIXME So much toJson...that happens in API atm too, but would be nice to avoid
    def getAll = repo.getAll.map(todos => Response.json(todos.toJson).setStatus(Status.OK))
    def getById(id: String) = for {
      longId <- toLongOrFail(id)
      entity <- repo.getById(longId)
    } yield {
      entity match {
        case None => Response.json(ErrorResponse(code = "not-found", details = s"Todo with id $longId was not found".some).toJson).setStatus(Status.NOT_FOUND)
        case Some(todo) => Response.json(todo.toJson).setStatus(Status.OK)
      }
    }

    def create(todo: Todo) = repo.create(todo).map(todo => Response.json(todo.toJson).setStatus(Status.OK))
    def update(id: String, todo: Todo) = for {
      longId <- toLongOrFail(id)
      entity <- repo.update(longId, todo)
    } yield {
      entity match {
        case None => Response.json(ErrorResponse(code = "not-found", details = s"Todo with id $longId was not found".some).toJson).setStatus(Status.NOT_FOUND)
        case Some(todo) => Response.json(todo.toJson).setStatus(Status.OK)
      }
    }
    def delete(id: String) = for {
      longId <- toLongOrFail(id)
      entity <- repo.delete(longId)
    } yield {
      entity match {
        case None => Response.json(ErrorResponse(code = "not-found", details = s"Todo with id $longId was not found".some).toJson).setStatus(Status.NOT_FOUND)
        case Some(todo) => Response.json(todo.toJson).setStatus(Status.OK)
      }
    }
  }

  val live = ZLayer.fromService[Repository[Long, Todo], TodoController.Service](repo => new Service(repo))

  def getAll: TodoControllerRIO = ZIO.accessM(_.get.getAll)
  def getById(id: String): TodoControllerRIO = ZIO.accessM(_.get.getById(id))
  def create(todo: Todo): TodoControllerRIO = ZIO.accessM(_.get.create(todo))
  def update(id: String, todo: Todo): TodoControllerRIO = ZIO.accessM(_.get.update(id, todo))
  def delete(id: String): TodoControllerRIO = ZIO.accessM(_.get.delete(id))
}
