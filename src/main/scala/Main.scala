import repository.{DataCategoryRepository, Database, Repository}
import zhttp.http.Http
import zhttp.service.Server
import zio._
import zio.blocking._
import configuration.ApplicationConfig._
import configuration.LoggingConfiguration
import doobie.util.log.LogHandler
import org.slf4j.{Logger, LoggerFactory}
import zio.console.putStrLn
import zio.logging._

object Main extends App {

  /* TODO
   - Really need to get all types in one spot it seems like...if not we'll be importing all over the place
   - logging (get doobie logging working too...ah...maybe it's jdbc logging that does that?...yeah logging still isn't at all right)......logging is so messed up
   - integrate circe (or look @ zio-json)
   - try to implement authentication (I think this will be hard and annoying)

   To fix: I think we can use package objects to help get type aliases all over the place? Honestly I don't really know what package objects do
   */

  val app = Http.text("Hello world!")

//  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] = {
//    Server.start(8080, app).exitCode
//  }

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] = {
    implicit val handle = LogHandler.jdkLogHandler
    val logger = LoggerFactory.getLogger("logger")
    logger.info("ehllo")
    val env = ((DatabaseConfig.live ++ Blocking.live) >>> Database.Connection.live >>> Repository.live >>> DataCategoryRepository.live) ++ zio.console.Console.live ++ LoggingConfiguration.live
    DataCategoryRepository.getById(3)
      .tap(dc => putStrLn(dc.toString))
      .tap(dc => log.info(s"Welll...here we are: $dc"))
      .provideLayer(env).exitCode
  }
}