resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

logLevel := Level.Warn

addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.5.3")
addSbtPlugin("com.github.mmizutani" % "sbt-play-gulp" % "0.1.1")

addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.3.5")


// Not necessary but useful for development
// https://github.com/jamesward/play-auto-refresh
addSbtPlugin("com.jamesward" % "play-auto-refresh" % "0.0.14")




