enablePlugins(GitVersioning)

name := "potassium"

organization in ThisBuild := "com.lynbrookrobotics"

val potassiumVersion = "0.1.0"
val isRelease = sys.props.get("TRAVIS_TAG").isDefined

git.formattedShaVersion := git.gitHeadCommit.value map { sha =>
  if (isRelease) potassiumVersion else s"$potassiumVersion-${sha.take(8)}"
}

scalaVersion in ThisBuild := "2.12.1"

resolvers in ThisBuild += "Funky-Repo" at "http://team846.github.io/repo"

lazy val sharedDependencies = Def.setting(Seq(
  "org.typelevel"  %%% "squants"  % "1.2.0-SNAPSHOT",
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
    testingJVM, testingJS,
    model,
    controlJVM, controlJS,
    remote,
    vision,
    frc,
    config,
    sensors,
    commonsJVM, commonsJS,
    lighting
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

lazy val testingJVM = testing.jvm
lazy val testingJS = testing.js

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

lazy val remote = project.dependsOn(coreJVM).settings(
  name := "potassium-remote",
  libraryDependencies ++= sharedDependencies.value,
  libraryDependencies ++= jvmDependencies
)

<<<<<<< HEAD
lazy val vision = project.dependsOn(coreJVM).settings(
  name := "potassium-vision",
  libraryDependencies ++= sharedDependencies.value,
  libraryDependencies ++= jvmDependencies
)

=======
>>>>>>> Uses WPILIB with SPI trait
lazy val frc = project.dependsOn(coreJVM, sensors).settings(
  name := "potassium-frc",
  libraryDependencies ++= sharedDependencies.value,
  libraryDependencies ++= jvmDependencies
)

lazy val config = project.dependsOn(coreJVM).settings(
  name := "potassium-config",
  libraryDependencies ++= sharedDependencies.value,
  libraryDependencies ++= jvmDependencies
)

lazy val lighting = project.dependsOn(coreJVM, testingJVM % Test).settings(
  name := "potassium-lighting",
  libraryDependencies ++= sharedDependencies.value,
  libraryDependencies ++= jvmDependencies
)

lazy val sensors = project.dependsOn(coreJVM).settings(
  name := "potassium-sensors",
  libraryDependencies ++= sharedDependencies.value,
  libraryDependencies ++= jvmDependencies
)

lazy val commons = crossProject.crossType(CrossType.Pure).
  dependsOn(control, testing % Test).settings(
  name := "potassium-commons",
  libraryDependencies ++= sharedDependencies.value
)

lazy val commonsJVM = commons.jvm
lazy val commonsJS = commons.js

publishArtifact := false

publishMavenStyle := true
publishTo in ThisBuild := Some(Resolver.file("gh-pages-repo", baseDirectory.value / "repo"))
