name := """betterplay"""

version := "0.9-SNAPSHOT"

scalaVersion := "2.13.5"

lazy val root = (project in file(".")).enablePlugins(PlayJava).settings(
  watchSources ++= (baseDirectory.value / "public/ui" ** "*").get
)


resolvers += "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"

// Required by specs2 to get scalaz-stream
resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"

Test / parallelExecution := false

//routesGenerator := InjectedRoutesGenerator

fork in run := true

Test / scalacOptions ++= Seq("-Yrangepos")

//val specs2Version = "4.1.0" //the playframework version is behind, upgrade not possible because errors
val specs2Version = "4.11.0"
val scalatestVersion = "3.2.8"

libraryDependencies ++= Seq(
  jdbc,
  ehcache,
  ws,
  filters,
  guice,
  "com.typesafe.play" %% "play-slick" % "4.0.2",
  "com.typesafe.play" %% "play-json" % "2.9.2", 
  "com.typesafe.slick" %% "slick" % "3.3.3",
  "com.typesafe.slick" %% "slick-hikaricp" % "3.3.3",
  "com.typesafe.akka" %% "akka-contrib" % "2.5.32" , //todo change throttler to stream
 // "org.specs2" %% "specs2-core" % specs2Version % "test",
   "org.specs2" %% "specs2-matcher-extra" % specs2Version % "test",
  "org.specs2" %% "specs2-scalacheck" % specs2Version % "test",
  "org.specs2" %% "specs2-junit" % specs2Version % "test",
 // "org.scalacheck" %% "scalacheck" % "1.14.0" % "test",
  "org.scalactic" %% "scalactic" % scalatestVersion, //for scalatest
  "org.scalatest" %% "scalatest-core" % scalatestVersion % "test", //for scalatest
  "org.scalatestplus.play" %% "scalatestplus-play" % "5.0.0" % "test", //scalatest
  "com.h2database" % "h2" % "1.4.200",
  "org.postgresql" % "postgresql" % "42.2.20",
  "org.scalaz" %% "scalaz-core" % "7.3.3",
  "org.jasypt" % "jasypt" % "1.9.3",
  "commons-io" % "commons-io" % "2.8.0",
  "com.andersen-gott" %% "scravatar" % "1.0.4",  
  "org.apache.poi" % "poi" % "5.0.0",
  "org.apache.poi" % "poi-ooxml" % "5.0.0",
  "net.sf.opencsv" % "opencsv" % "2.3",
  "org.apache.commons" % "commons-email" % "1.5" 

)

libraryDependencies += specs2 % Test

coverageEnabled := false

// Compile the project before generating Eclipse files, so that generated .scala or .class files for views and routes are present
//EclipseKeys.preTasks := Seq(compile in Compile, compile in Test)

