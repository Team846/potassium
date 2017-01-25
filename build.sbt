name := "potassium"

organization in ThisBuild := "com.lynbrookrobotics"

version in ThisBuild := "0.1.0-SNAPSHOT"

scalaVersion in ThisBuild := "2.12.1"

crossScalaVersions in ThisBuild := Seq("2.12.1")

lazy val sharedDependencies = Def.setting(Seq(
  "org.typelevel"  %%% "squants"  % "1.0.0",
  "org.scalatest" %%% "scalatest" % "3.0.1" % Test,
  "org.scalacheck" %%% "scalacheck" % "1.13.4" % Test
))

lazy val jvmDependencies = Seq(
  "org.mockito" % "mockito-core" % "2.3.11" % Test,
  "com.storm-enroute" %% "scalameter-core" % "0.8.2" % Test
)

parallelExecution in ThisBuild := false

lazy val potassium = project.in(file(".")).
  aggregate(
    coreJVM, coreJS,
    controlJVM, controlJS,
    testingJVM, testingJS,
    frc,
    config,
    sensorsJVM, sensorsJS,
    commonsJVM, commonsJS
  ).settings(
    publish := {},
    publishLocal := {}
  )

lazy val core = crossProject.crossType(CrossType.Full).settings(
  name := "potassium-core",
  libraryDependencies ++= sharedDependencies.value
).jvmSettings(libraryDependencies ++= jvmDependencies).jsSettings(
  requiresDOM := true,
  coverageEnabled := false
)

lazy val coreJVM = core.jvm
lazy val coreJS = core.js

lazy val testing = crossProject.crossType(CrossType.Pure).dependsOn(core).settings(
  name := "potassium-testing",
  libraryDependencies ++= sharedDependencies.value
)

lazy val model = project.dependsOn(coreJVM).settings(
  name := "potassium-model",
  libraryDependencies ++= sharedDependencies.value,
  libraryDependencies ++= jvmDependencies
)

lazy val control = crossProject.crossType(CrossType.Pure).dependsOn(core).settings(
  name := "potassium-control",
  libraryDependencies ++= sharedDependencies.value
)

lazy val controlJVM = control.jvm
lazy val controlJS = control.js

lazy val testingJVM = testing.jvm
lazy val testingJS = testing.js

lazy val frc = project.dependsOn(coreJVM).settings(
  name := "potassium-frc",
  libraryDependencies ++= sharedDependencies.value,
  libraryDependencies ++= jvmDependencies
)

lazy val config = project.dependsOn(coreJVM).settings(
  name := "potassium-config",
  libraryDependencies ++= sharedDependencies.value,
  libraryDependencies ++= jvmDependencies
)

lazy val sensors = crossProject.crossType(CrossType.Pure).dependsOn(core).settings(
  name := "potassium-sensors",
  libraryDependencies ++= sharedDependencies.value
)

lazy val sensorsJVM = sensors.jvm
lazy val sensorsJS = sensors.js

lazy val commons = crossProject.crossType(CrossType.Pure).dependsOn(control, sensors).settings(
  name := "potassium-commons",
  libraryDependencies ++= sharedDependencies.value
)

lazy val commonsJVM = commons.jvm
lazy val commonsJS = commons.js

publishArtifact := false

publishMavenStyle := true
publishTo in ThisBuild := Some(Resolver.file("gh-pages-repo", baseDirectory.value / "repo"))
