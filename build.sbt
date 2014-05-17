name := """betterplay"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.10.4"

libraryDependencies ++= Seq(
  jdbc,
  cache,
  ws,
  "com.typesafe.play" %% "play-slick" % "0.6.0.1",
  "joda-time" % "joda-time" % "2.3",
  "org.joda" % "joda-convert" % "1.6"
)



