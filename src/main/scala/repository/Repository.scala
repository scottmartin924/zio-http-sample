package repository

import doobie.{ConnectionIO, Transactor}
import zio.{Has, Task, ZLayer}
import zio.interop.catz._

object Repository {
  type ExecutorEnv = Has[Executor]
  trait Executor {
    // FIXME These are awful...could be a lot smarter...query and update could be separated though using Query0 and Update0 maybe?
    //  def query[A](fragment: Fragment): Task[A]
    //  def update[A](fragment: Fragment): Task[A]
    def execute[A](io: ConnectionIO[A]): Task[A]
  }

  class DoobieExecutor(xa: Transactor[Task]) extends Executor {
    override def execute[A](io: ConnectionIO[A]): Task[A] = {
      xa.trans.apply(io)
    }
  }

  val live: ZLayer[Has[Transactor[Task]], Throwable, ExecutorEnv] = ZLayer.fromService[Transactor[Task], Repository.Executor](xa => new DoobieExecutor(xa))
}