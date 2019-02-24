name := """wuekabel"""

organization := "de.uniwue"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala).settings(packageName in Universal := s"${name.value}")

scalaVersion := "2.12.8"

updateOptions := updateOptions.value.withCachedResolution(cachedResoluton = true)
resolveFromWebjarsNodeModulesDir := true

val webJarDependencies = Seq(
  "org.webjars.npm" % "jquery" % "3.3.1",
  "org.webjars.npm" % "types__jquery" % "3.3.29",

  "org.webjars.npm" % "materialize-css" % "1.0.0",

  // javascript db
  "org.webjars.npm" % "dexie" % "2.0.4"

)

dependencyOverrides ++= Seq(
  "org.webjars.npm" % "types__sizzle" % "2.3.2"
)


libraryDependencies ++= webJarDependencies


libraryDependencies ++= Seq(
  guice,

  "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.2" % Test,

  "mysql" % "mysql-connector-java" % "8.0.13",


  // Better enums for scala
  "com.beachape" %% "enumeratum-play" % "1.5.14",
  "com.beachape" %% "enumeratum-play-json" % "1.5.14",


  "com.typesafe.play" %% "play-slick" % "3.0.3",
  "com.typesafe.play" %% "play-slick-evolutions" % "3.0.3",

  "com.github.t3hnar" %% "scala-bcrypt" % "3.1",

  // Betterfiles
  "com.github.pathikrit" %% "better-files" % "3.7.0",

  "org.apache.poi" % "poi" % "4.0.1",
  "org.apache.poi" % "poi-ooxml" % "4.0.1"
)

// Adds additional packages into Twirl
//TwirlKeys.templateImports += "de.uniwue.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "de.uniwue.binders._"
