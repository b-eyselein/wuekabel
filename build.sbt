name := """wuekabel"""

organization := "de.uniwue"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)
  .settings(packageName in Universal := s"${name.value}")

TwirlKeys.templateImports += "model.TemplateConsts._"

scalaVersion := "2.13.1"

updateOptions := updateOptions.value.withCachedResolution(cachedResoluton = true)

val webJarDependencies = Seq(
  // FIXME: remove jquery?!?
  "org.webjars.npm" % "jquery" % "3.4.1",
  "org.webjars.npm" % "types__jquery" % "3.3.31",

  "org.webjars.npm" % "materialize-css" % "1.0.0",
  "org.webjars.npm" % "types__materialize-css" % "1.0.6"
)

dependencyOverrides ++= Seq(
  "org.webjars.npm" % "types__sizzle" % "2.3.2",
  "org.webjars.npm" % "types__cash" % "0.0.3",
)

PlayKeys.playDefaultPort := 9090

libraryDependencies ++= webJarDependencies

libraryDependencies ++= Seq(
  guice,

  "org.scalatestplus.play" %% "scalatestplus-play" % "4.0.3" % Test,

  "mysql" % "mysql-connector-java" % "8.0.17",

  // Better enums for scala
  "com.beachape" %% "enumeratum-play" % "1.5.16",
  "com.beachape" %% "enumeratum-play-json" % "1.5.16",


  "com.typesafe.play" %% "play-slick" % "4.0.2",
  "com.typesafe.play" %% "play-slick-evolutions" % "4.0.2",

  "com.github.t3hnar" %% "scala-bcrypt" % "4.1",

  // Betterfiles
  "com.github.pathikrit" %% "better-files" % "3.8.0",

  "org.apache.poi" % "poi" % "4.1.0",
  "org.apache.poi" % "poi-ooxml" % "4.1.0"
)
