name := "potassium"

organization in ThisBuild := "com.lynbrookrobotics"

version in ThisBuild := "0.1.0-SNAPSHOT"

scalaVersion in ThisBuild := "2.11.8"

lazy val core = project

lazy val testing = project.dependsOn(core)

lazy val frc = project.dependsOn(core)

lazy val sensors = project.dependsOn(core)

lazy val commons = project.dependsOn(sensors)

publishArtifact := false

publishMavenStyle := true
publishTo in ThisBuild := Some(Resolver.file("gh-pages-repo", baseDirectory.value / "repo"))
