name := "TWM-ZIO"

version := "0.1"

scalaVersion := "2.13.1"

libraryDependencies += "dev.zio" %% "zio" % "1.0.0-RC17"
libraryDependencies += "com.softwaremill.sttp.client" %% "async-http-client-backend-zio" % "2.0.1"
libraryDependencies += "com.softwaremill.sttp.client" %% "circe" % "2.0.1"
libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.3"

val circeVersion = "0.13.0"

libraryDependencies ++= Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-parser",
  "io.circe" %% "circe-optics"
).map(_ % circeVersion)

addCompilerPlugin("com.olegpy"    %% "better-monadic-for" % "0.3.1")

scalacOptions ++= Seq("-deprecation")