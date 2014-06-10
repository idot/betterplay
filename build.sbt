name := """betterplay"""

version := "0.7-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.10.4"

resolvers += "Sonatype Snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/"

resolvers += "thirdpary" at "http://mammut:8082/nexus/content/repositories/hirdparty"

parallelExecution in Test := false


libraryDependencies ++= Seq(
  jdbc,
  cache,
  ws,
  filters,
  "com.typesafe.play" %% "play-slick" % "0.7.0-M1",
  "joda-time" % "joda-time" % "2.3",
  "org.joda" % "joda-convert" % "1.6",
  "org.specs2" %% "specs2" % "2.3.12" % "test",
  "org.specs2" %% "specs2-matcher-extra" % "2.3.12" % "test",
  "org.specs2" %% "specs2-scalacheck" % "2.3.12" % "test",
  "org.scalacheck" %% "scalacheck" % "1.11.4" % "test",
  "org.postgresql" % "postgresql" % "9.2-1003-jdbc4",
  "org.scalaz" %% "scalaz-core" % "7.0.6",
  "org.jasypt" % "jasypt" % "1.9.2",
  "commons-io" % "commons-io" % "2.4",
  "com.andersen-gott" %% "scravatar" % "1.0.2",  
  "org.apache.poi" % "poi" % "3.10-FINAL",
  "net.sf.opencsv" % "opencsv" % "2.3",
  "org.webjars" % "angularjs" % "1.2.16-2",  
  "org.webjars" % "requirejs" % "2.1.11-1",
  "org.webjars" % "restangular" % "1.4.0-2",
  "org.webjars" % "underscorejs" % "1.6.0-3",
  "org.webjars" % "angular-ui" % "0.4.0-3",
  "org.webjars" % "angular-ui-bootstrap" % "0.11.0-2",
  "org.webjars" % "angular-ui-router" % "0.2.10-1",
  "org.webjars" % "ng-table" % "0.3.2",
  "org.webjars" % "momentjs" % "2.6.0-2",
  "org.webjars" % "font-awesome" % "4.1.0",
  "org.webjars" % "angular-ui-utils" % "47ff7ef35c",
  "org.webjars" % "angularjs-nvd3-directives" % "0.0.7-1" 
//,
//  "com.typesafe" %% "play-plugins-mailer" % "2.0.4"
)


pipelineStages := Seq(rjs, digest, gzip)
