resolvers += "Typesafe repository" at "https://repo.typesafe.com/typesafe/releases/"

logLevel := Level.Warn

addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.8.8")
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.5.1")
