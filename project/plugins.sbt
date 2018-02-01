resolvers += "Funky-Repo" at "http://lynbrookrobotics.com/repo"

addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.5.0")

addSbtPlugin("org.scoverage" % "sbt-coveralls" % "1.1.0")

addSbtPlugin("org.scalastyle" %% "scalastyle-sbt-plugin" % "0.8.0")

addSbtPlugin("com.lynbrookrobotics" % "travis-scalastyle" % "0.2.0")

addSbtPlugin("org.scala-js" % "sbt-scalajs" % "0.6.21")

addSbtPlugin("org.portable-scala" % "sbt-crossproject" % "0.3.0")

addSbtPlugin("org.portable-scala" % "sbt-scalajs-crossproject" % "0.3.0")

if (System.getenv("NATIVE_TARGET") == "ARM32") {
  addSbtPlugin("org.scala-native" % "sbt-scala-native" % "0.3.7-arm-jni-threads" exclude("org.scala-native", "sbt-crossproject"))
} else {
  addSbtPlugin("org.scala-native" % "sbt-scala-native" % "0.3.6" exclude("org.scala-native", "sbt-crossproject"))
}

addSbtPlugin("com.typesafe.sbt" % "sbt-git" % "0.8.5")

addSbtPlugin("com.47deg"  % "sbt-microsites" % "0.6.1")

addSbtPlugin("com.eed3si9n" % "sbt-unidoc" % "0.4.0")
