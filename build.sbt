import PlayGulpPlugin._
import PlayGulpKeys._

name := """betterplay"""

version := "0.8-SNAPSHOT"

scalaVersion := "2.11.8"



lazy val root = (project in file("."))
  .enablePlugins(PlayScala)



resolvers += "Sonatype Snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/"

resolvers += "thirdpary" at "http://mammut:8082/nexus/content/repositories/thirdparty"

// Required by specs2 to get scalaz-stream
resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"


parallelExecution in Test := false

routesGenerator := InjectedRoutesGenerator

fork in run := true

scalacOptions in Test ++= Seq("-Yrangepos")

libraryDependencies ++= Seq(
  jdbc,
  cache,
  ws,
  filters,
  "com.typesafe.play" %% "play-slick" % "2.0.0",
  "com.typesafe.slick" %% "slick" % "3.1.1",
  "joda-time" % "joda-time" % "2.3",
  "org.joda" % "joda-convert" % "1.6",
  "org.specs2" %% "specs2-core" % "3.7.2" % "test",
  "org.specs2" %% "specs2-matcher-extra" % "3.7.2" % "test",
  "org.specs2" %% "specs2-scalacheck" % "3.7.2" % "test",
  "org.scalacheck" %% "scalacheck" % "1.11.4" % "test",
  "org.scalactic" %% "scalactic" % "2.2.6", //for scalatest
  "org.scalatest" %% "scalatest" % "2.2.6" % "test", //for scalatest
  "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.0-RC1" % "test", //scalatest
  "com.h2database" % "h2" % "1.4.191",
  "org.postgresql" % "postgresql" % "9.2-1003-jdbc4",
  "org.scalaz" %% "scalaz-core" % "7.2.2",
  "org.jasypt" % "jasypt" % "1.9.2",
  "commons-io" % "commons-io" % "2.4",
  "com.andersen-gott" %% "scravatar" % "1.0.3",  
  "org.apache.poi" % "poi" % "3.10-FINAL",
  "net.sf.opencsv" % "opencsv" % "2.3",
  "com.typesafe.play" %% "play-mailer" % "5.0.0-M1"


)

libraryDependencies += specs2 % Test

coverageEnabled := false

PlayGulpPlugin.playGulpSettings ++ PlayGulpPlugin.withTemplates
