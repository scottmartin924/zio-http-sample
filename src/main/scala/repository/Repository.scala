package repository

import doobie.implicits._
import doobie.{ConnectionIO, Transactor}
import resource.Todo
import zio.{Has, Task, ZLayer}
import zio.interop.catz._

trait Repository[K,V] {
  def getAll: Task[List[V]]
  def getById(id: K): Task[Option[V]]
  def create(value: V): Task[V]
  def update(key: K, value: V): Task[Option[V]]
  def delete(key: K): Task[Option[V]]
}

object Repository {
  type ExecutorEnv = Has[Executor]
  trait Executor {
    def execute[A](io: ConnectionIO[A]): Task[A]
  }

  class DoobieExecutor(xa: Transactor[Task]) extends Executor {
    override def execute[A](io: ConnectionIO[A]): Task[A] = io.transact(xa)
  }

  val live: ZLayer[Has[Transactor[Task]], Throwable, ExecutorEnv] = ZLayer.fromService[Transactor[Task], Repository.Executor](xa => new DoobieExecutor(xa))
}