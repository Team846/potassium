resolvers += "Funky-Repo" at "http://team846.github.io/repo"

addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.5.0")

addSbtPlugin("org.scoverage" % "sbt-coveralls" % "1.1.0")

addSbtPlugin("org.scalastyle" %% "scalastyle-sbt-plugin" % "0.8.0")

addSbtPlugin("com.lynbrookrobotics" % "travis-scalastyle" % "0.2.0-SNAPSHOT")

addSbtPlugin("org.scala-js" % "sbt-scalajs" % "0.6.16")

addSbtPlugin("org.scala-native" % "sbt-crossproject" % "0.1.0")

addSbtPlugin("org.scala-native" % "sbt-scalajs-crossproject" % "0.1.0")

addSbtPlugin("org.scala-native" % "sbt-scala-native" % "0.2.1")

addSbtPlugin("com.typesafe.sbt" % "sbt-git" % "0.8.5")
