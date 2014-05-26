name := """betterplay"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.10.4"

resolvers += "Sonatype Snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/"

parallelExecution in Test := false

libraryDependencies ++= Seq(
  jdbc,
  cache,
  ws,
  "com.typesafe.play" %% "play-slick" % "0.7.0-SNAPSHOT",
  "joda-time" % "joda-time" % "2.3",
  "org.joda" % "joda-convert" % "1.6",
  "org.scalacheck" %% "scalacheck" % "1.11.4" % "test",
  "org.postgresql" % "postgresql" % "9.2-1003-jdbc4",
  "org.scalaz" %% "scalaz-core" % "7.0.6",
  "org.jasypt" % "jasypt" % "1.9.2",
  "commons-io" % "commons-io" % "2.4",
  "org.webjars" % "angularjs" % "1.2.16-2",  
  "org.webjars" % "requirejs" % "2.1.11-1",
  "org.webjars" % "restangular" % "1.4.0-2",
  "org.webjars" % "underscorejs" % "1.6.0-3",
  "org.webjars" % "angular-ui" % "0.4.0-3",
  "org.webjars" % "angular-ui-bootstrap" % "0.11.0-2",
  "org.webjars" % "angular-ui-router" % "0.2.10-1"
)


pipelineStages := Seq(rjs, digest, gzip)
