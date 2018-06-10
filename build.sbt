name := """betterplay"""

version := "0.9-SNAPSHOT"

scalaVersion := "2.12.4"

lazy val root = (project in file(".")).enablePlugins(PlayJava).settings(
  watchSources ++= (baseDirectory.value / "public/ui" ** "*").get
)


resolvers += "Sonatype Snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/"

//resolvers += "thirdpary" at "http://mammut:8082/nexus/content/repositories/thirdparty"

// Required by specs2 to get scalaz-stream
resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"

parallelExecution in Test := false

//routesGenerator := InjectedRoutesGenerator

fork in run := true

scalacOptions in Test ++= Seq("-Yrangepos")

//val specs2Version = "4.1.0" //the playframework version is behind, upgrade not possible because errors
val specs2Version = "3.8.9"


libraryDependencies ++= Seq(
  jdbc,
  ehcache,
  ws,
  filters,
  guice,
  "com.typesafe.play" %% "play-slick" % "3.0.1",
  "com.typesafe.play" %% "play-json" % "2.6.0", 
  "com.typesafe.slick" %% "slick" % "3.2.3",
  "com.typesafe.slick" %% "slick-hikaricp" % "3.2.3",
  "com.typesafe.akka" %% "akka-contrib" % "2.5.12" , //todo change throttler to stream
 // "org.specs2" %% "specs2-core" % specs2Version % "test",
   "org.specs2" %% "specs2-matcher-extra" % specs2Version % "test",
  "org.specs2" %% "specs2-scalacheck" % specs2Version % "test",
  "org.specs2" %% "specs2-junit" % specs2Version % "test",
 // "org.scalacheck" %% "scalacheck" % "1.14.0" % "test",
  "org.scalactic" %% "scalactic" % "3.0.5", //for scalatest
  "org.scalatest" %% "scalatest" % "3.0.5" % "test", //for scalatest
  "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.2" % "test", //scalatest
  "com.h2database" % "h2" % "1.4.191",
  "org.postgresql" % "postgresql" % "9.2-1003-jdbc4",
  "org.scalaz" %% "scalaz-core" % "7.2.22",
  "org.jasypt" % "jasypt" % "1.9.2",
  "commons-io" % "commons-io" % "2.6",
  "com.andersen-gott" %% "scravatar" % "1.0.3",  
  "org.apache.poi" % "poi" % "3.14",
  "org.apache.poi" % "poi-ooxml" % "3.14",
  "net.sf.opencsv" % "opencsv" % "2.3",
  "org.apache.commons" % "commons-email" % "1.4" 

)

libraryDependencies += specs2 % Test

coverageEnabled := false

// Compile the project before generating Eclipse files, so that generated .scala or .class files for views and routes are present
//EclipseKeys.preTasks := Seq(compile in Compile, compile in Test)

