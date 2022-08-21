import sbt.Keys.libraryDependencies

ThisBuild / scalaVersion := "2.13.8"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "com.example"
ThisBuild / organizationName := "example"

val zioVersion = "1.0.13"
val zHttpVersion = "1.0.0.0-RC22"
lazy val root = (project in file("."))
  .settings(
    name := "zio-http-sample",
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio" % zioVersion,
      "io.d11" %% "zhttp" % zHttpVersion,
      "dev.zio" %% "zio-config" % "2.0.0",
      "io.github.kitlangton" %% "zio-magic" % "0.3.11",
      "org.tpolecat" %% "doobie-core" % "0.13.4",
      "org.tpolecat" %% "doobie-postgres" % "0.13.4",
      "org.tpolecat" %% "doobie-postgres-circe" % "0.13.4",
      "org.tpolecat" %% "doobie-hikari" % "0.13.4",
      "dev.zio" %% "zio-interop-cats" % "2.1.4.0",
      "dev.zio" %% "zio-config" % "2.0.0",
      "dev.zio" %% "zio-config-magnolia" % "2.0.0",
      "dev.zio" %% "zio-logging-slf4j" % "0.5.14",
      "org.apache.logging.log4j" % "log4j-slf4j-impl" % "2.17.0",
      "org.apache.logging.log4j" % "log4j-api" % "2.17.0",
      "org.apache.logging.log4j" % "log4j-core" % "2.17.0",
      "com.github.jwt-scala" %% "jwt-core" % "9.0.4", // Honestly no idea if this is a good jwt library to use
      "io.circe" %% "circe-core" % "0.14.1",
      "io.circe" %% "circe-parser" % "0.14.1",
      "io.circe" %% "circe-optics" % "0.14.1",
      "io.circe" %% "circe-generic" % "0.14.1",
      "dev.zio" %% "zio-test" % zioVersion % Test,
      "io.d11" %% "zhttp-test" % zHttpVersion % Test,
      "dev.zio" %% "zio-test-sbt" % zioVersion % Test
    ),
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
  )
