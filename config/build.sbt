resolvers in ThisBuild += "Funky-Repo" at "http://team846.github.io/repo"

libraryDependencies += "com.chuusai" %% "shapeless" % "2.3.3"
libraryDependencies += "io.argonaut" %% "argonaut" % "6.2.1"
libraryDependencies += "com.github.alexarchambault" %% "argonaut-shapeless_6.2" % "1.2.0-M8"

libraryDependencies += "org.scala-lang" % "scala-reflect" % scalaVersion.value
