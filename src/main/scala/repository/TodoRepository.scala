package repository

import resource.Todo
import zio.{Has, RIO, ZIO, ZLayer}
import repository.Repository.ExecutorEnv

object TodoRepository extends DoobieLogging {
  type TodoRepoEnv = Has[Repository[Long, Todo]]

  class Service(db: Repository.Executor) extends Repository[Long, Todo] {
    override def getAll = db.execute(Todo.getAll)
    override def getById(id: Long) = db.execute(Todo.getById(id))
    override def create(value: Todo) = db.execute(Todo.create(value))
    override def update(id: Long, value: Todo) =
      db.execute(Todo.update(id, value))
    override def delete(key: Long) = db.execute(Todo.delete(key))
  }

  def getAll: RIO[TodoRepoEnv, List[Todo]] = ZIO.accessM(_.get.getAll)
  def getById(id: Long): RIO[TodoRepoEnv, Option[Todo]] =
    ZIO.accessM(_.get.getById(id))
  def create(todo: Todo): RIO[TodoRepoEnv, Todo] =
    ZIO.accessM(_.get.create(todo))
  def update(id: Long, todo: Todo): RIO[TodoRepoEnv, Option[Todo]] =
    ZIO.accessM(_.get.update(id, todo))
  def delete(id: Long): RIO[TodoRepoEnv, Option[Todo]] =
    ZIO.accessM(_.get.delete(id))

  val live: ZLayer[ExecutorEnv, Throwable, TodoRepoEnv] =
    ZLayer.fromService[Repository.Executor, Repository[Long, Todo]](db =>
      new Service(db)
    )
}
