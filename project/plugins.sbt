resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

logLevel := Level.Warn

addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.6.15")
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.5.1")
