name := """wuekabel"""

organization := "de.uniwue"

version := "1.0-SNAPSHOT"

lazy val root = (project in file("."))
  .enablePlugins(PlayScala)
  .settings(Universal / packageName := s"${name.value}")

TwirlKeys.templateImports += "model.TemplateConsts._"

scalaVersion := "2.13.6"

// updateOptions := updateOptions.value.withCachedResolution(cachedResoluton = true)

val webJarDependencies = Seq(
  // FIXME: remove jquery?!?
  "org.webjars.npm" % "jquery"                 % "3.6.0",
  "org.webjars.npm" % "types__jquery"          % "3.5.6",
  "org.webjars.npm" % "materialize-css"        % "1.0.0",
  "org.webjars.npm" % "types__materialize-css" % "1.0.6"
)

dependencyOverrides ++= Seq(
  "org.webjars.npm" % "types__sizzle" % "2.3.2",
  "org.webjars.npm" % "types__cash"   % "0.0.3"
)

PlayKeys.playDefaultPort := 9090

libraryDependencies ++= webJarDependencies

libraryDependencies ++= Seq(
  guice,
  "org.scalatestplus.play" %% "scalatestplus-play"   % "5.1.0" % Test,
  "mysql"                   % "mysql-connector-java" % "8.0.26",
  // Better enums for scala
  "com.beachape"      %% "enumeratum-play"       % "1.7.0",
  "com.beachape"      %% "enumeratum-play-json"  % "1.7.0",
  "com.typesafe.play" %% "play-slick"            % "5.0.0",
  "com.typesafe.play" %% "play-slick-evolutions" % "5.0.0",
  "com.github.t3hnar" %% "scala-bcrypt"          % "4.3.0",
  // Betterfiles
  "com.github.pathikrit" %% "better-files" % "3.9.1",
  "org.apache.poi"        % "poi"          % "5.0.0",
  "org.apache.poi"        % "poi-ooxml"    % "5.0.0"
)
