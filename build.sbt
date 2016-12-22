name := "potassium"

organization in ThisBuild := "com.lynbrookrobotics"

version in ThisBuild := "0.1.0-SNAPSHOT"

scalaVersion in ThisBuild := "2.12.1"

coverageEnabled in ThisBuild := true

libraryDependencies in ThisBuild ++= Seq(
  "org.typelevel"  %% "squants"  % "1.0.0",
  "org.scalatest" %% "scalatest" % "3.0.1" % Test,
  "org.scalacheck" %% "scalacheck" % "1.13.4" % Test,
  "org.mockito" % "mockito-core" % "2.3.11" % Test,
  "com.storm-enroute" %% "scalameter-core" % "0.8.2" % Test
)

parallelExecution in ThisBuild := false

lazy val core = project

lazy val testing = project.dependsOn(core)

lazy val frc = project.dependsOn(core)

lazy val sensors = project.dependsOn(core)

lazy val commons = project.dependsOn(sensors)

publishArtifact := false

publishMavenStyle := true
publishTo in ThisBuild := Some(Resolver.file("gh-pages-repo", baseDirectory.value / "repo"))
