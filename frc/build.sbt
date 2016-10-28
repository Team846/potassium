name := "potassium-frc"

resolvers += "WPILib-Maven" at "http://team846.github.io/wpilib-maven"

libraryDependencies += "edu.wpi.first" % "wpilib" % "0.1.0.201603020231"
libraryDependencies += "edu.wpi.first" % "networktables" % "0.1.0.201603020231"

libraryDependencies += "org.mockito" % "mockito-core" % "2.2.9" % Test
libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.0" % Test
libraryDependencies += "org.scalacheck" %% "scalacheck" % "1.13.0" % Test