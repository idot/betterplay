name := """betterplay"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.10.4"

resolvers += "Sonatype Snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/"

libraryDependencies ++= Seq(
  jdbc,
  cache,
  ws,
  "com.typesafe.play" %% "play-slick" % "0.6.0.1",
  "joda-time" % "joda-time" % "2.3",
  "org.joda" % "joda-convert" % "1.6",
  "org.scalacheck" %% "scalacheck" % "1.11.4" % "test",
  "org.postgresql" % "postgresql" % "9.2-1003-jdbc4",
  "org.scalaz" %% "scalaz-core" % "7.0.6",
   "com.mohiva" %% "play-silhouette" % "1.0-SNAPSHOT"
)


