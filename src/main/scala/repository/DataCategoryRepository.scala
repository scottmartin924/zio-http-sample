package repository

import doobie.ConnectionIO
import doobie.implicits.toSqlInterpolator
import resource.DataCategory
import zio.{Has, RIO, Task, ZIO, ZLayer}

// FIXME Naming? Make it named after whatever object type we're getting
object DataCategoryRepository {

  type DataCategoryRepoEnv = Has[DataCategoryRepository.Service]

  // FIXME Repository can probably be a common trait
  trait Repository {
    def getAll: Task[List[DataCategory]]
    def getById(id: Int): Task[Option[DataCategory]]
    def create(category: DataCategory): Task[DataCategory]
  }

  // NOTE: Could split the SQL out into its own object...but to me that sort of seems like overkill unless we need to reuse it...oh which we might in the service?
  // Yeah, I can see the usefulness of having something that returns connectionIos so we can get transactions??
  class Service(db: Repository.Executor) extends Repository {
    // FIXME These feel wrong...all db.execute seems strange?? Maybe common repos can just return connectionio and wrap that in execute automatically?

    // Well...this is very simple for this case, but in the scenario where this service actually has logic to implement lots
    // of things could be wrapped up into one transaction
    override def getAll: Task[List[DataCategory]] = db.execute(Sql.getAll)
    override def getById(id: Int): Task[Option[DataCategory]] = db.execute(Sql.getById(id))
    override def create(category: DataCategory): Task[DataCategory] = db.execute(Sql.create(category))
  }

  def getAll: RIO[DataCategoryRepoEnv, List[DataCategory]] = ZIO.accessM(_.get.getAll)
  def getById(id: Int): RIO[DataCategoryRepoEnv, Option[DataCategory]] = ZIO.accessM(_.get.getById(id))
  def create(category: DataCategory): RIO[DataCategoryRepoEnv, DataCategory] = ZIO.accessM(_.get.create(category))

  val live: ZLayer[Has[Repository.Executor], Throwable, DataCategoryRepoEnv] = ZLayer.fromService[Repository.Executor, DataCategoryRepository.Service](db => new Service(db))

  // Really this becomes the repository in itself I think since it gives connectionios back..maybe
  // TODO Consider if it's better to give back Query0 and Update0 type things here???
  object Sql {
    def getAll: ConnectionIO[List[DataCategory]] = sql"""select id, data_name from data_category""".query[DataCategory].to[List]

    def getById(id: Int): ConnectionIO[Option[DataCategory]] =
      sql"""select id, data_name from data_category
           |where id = $id
         """.stripMargin.query[DataCategory].option

    def create(category: DataCategory): ConnectionIO[DataCategory] =
      sql"""
        INSERT into data_category(id, data_name)
        values (${category.id}, ${category.name})
         """.update.run.map(_ => category) // fIXME probably wrong
  }
}
