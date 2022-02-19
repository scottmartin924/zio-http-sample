package configuration

import zio.config._
import ConfigDescriptor._
import ConfigSource._
import zio.{Has, ZLayer}
import zio.config.magnolia.descriptor

object ApplicationConfig {

  case class DatabaseConfig(connectionString: String, driver: String, username: String, password: String)
  object DatabaseConfig {
    type DbConfigEnv = Has[DatabaseConfig]
    private val dbConfig: ConfigDescriptor[DatabaseConfig] = descriptor[DatabaseConfig]
    val live = ZConfig.fromPropertiesFile("conf/application.properties", dbConfig)
  }
}
