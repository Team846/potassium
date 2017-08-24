import sbtcrossproject.{crossProject, CrossType, CrossClasspathDependency}

enablePlugins(GitVersioning, TravisScalaStylePlugin)

name := "potassium"

organization in ThisBuild := "com.lynbrookrobotics"

val potassiumVersion = "0.1.0"
val isRelease = sys.props.get("TRAVIS_TAG").isDefined

git.formattedShaVersion := git.gitHeadCommit.value map { sha =>
  if (isRelease) potassiumVersion else s"$potassiumVersion-${sha.take(8)}"
}

scalaVersion in ThisBuild := "2.12.1"

resolvers in ThisBuild += "Funky-Repo" at "http://team846.github.io/repo"

parallelExecution in Test in ThisBuild := false

lazy val sharedDependencies = Def.setting(Seq(
  "org.typelevel"  %%% "squants"  % "1.3.0",
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
    coreJVM, coreJS, coreNative,
    model,
    controlJVM, controlJS, controlNative,
    remote,
    vision,
    frc,
    config,
    sensors,
    commonsJVM, commonsJS, commonsNative,
    lighting
  ).settings(
  publish := {},
  publishLocal := {}
)

lazy val nativeSettings = Def.settings(
  scalaVersion := "2.11.11",
  libraryDependencies := libraryDependencies.value.filterNot(_.configurations.exists(_ == Test.name)),
  test := {
    (compile in Compile).value
  }
)

lazy val core = crossProject(JSPlatform, JVMPlatform, NativePlatform).crossType(CrossType.Full).settings(
  name := "potassium-core",
  libraryDependencies ++= sharedDependencies.value
).jvmSettings(libraryDependencies ++= jvmDependencies).jsSettings(
  requiresDOM := true,
  coverageEnabled := false
).nativeSettings(nativeSettings)

lazy val coreJVM = core.jvm
lazy val coreJS = core.js
lazy val coreNative = core.native

lazy val model = project.dependsOn(coreJVM, commonsJVM).settings(
  name := "potassium-model",
  libraryDependencies ++= sharedDependencies.value,
  libraryDependencies ++= jvmDependencies
)

lazy val control = crossProject(JSPlatform, JVMPlatform, NativePlatform).crossType(CrossType.Pure).dependsOn(core).settings(
  name := "potassium-control",
  libraryDependencies ++= sharedDependencies.value
).nativeSettings(nativeSettings)

lazy val controlJVM = control.jvm
lazy val controlJS = control.js
lazy val controlNative = control.native

lazy val remote = project.dependsOn(coreJVM).settings(
  name := "potassium-remote",
  libraryDependencies ++= sharedDependencies.value,
  libraryDependencies ++= jvmDependencies
)

lazy val vision = project.dependsOn(coreJVM).settings(
  name := "potassium-vision",
  libraryDependencies ++= sharedDependencies.value,
  libraryDependencies ++= jvmDependencies
)

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

lazy val lighting = project.dependsOn(coreJVM).settings(
  name := "potassium-lighting",
  libraryDependencies ++= sharedDependencies.value,
  libraryDependencies ++= jvmDependencies
)

lazy val sensors = project.dependsOn(coreJVM).settings(
  name := "potassium-sensors",
  libraryDependencies ++= sharedDependencies.value,
  libraryDependencies ++= jvmDependencies
)

lazy val commons = crossProject(JSPlatform, JVMPlatform, NativePlatform).crossType(CrossType.Pure).
  dependsOn(
    control
  ).settings(
  name := "potassium-commons",
  libraryDependencies ++= sharedDependencies.value
).nativeSettings(nativeSettings)


lazy val commonsJVM = commons.jvm
lazy val commonsJS = commons.js
lazy val commonsNative = commons.native

lazy val docsMappingsAPIDir = settingKey[String]("Name of subdirectory in site target directory for api docs")

lazy val docs = project
  .enablePlugins(ScalaUnidocPlugin)
  .dependsOn(coreJVM, model, controlJVM,
    remote, vision, frc, config, sensors,
    commonsJVM, lighting)
  .settings(
    autoAPIMappings := true,
    docsMappingsAPIDir := "api",
    addMappingsToSiteDir(mappings in (ScalaUnidoc, packageDoc), docsMappingsAPIDir),
    unidocProjectFilter in (ScalaUnidoc, unidoc) :=
      inProjects(coreJVM, model, controlJVM,
        remote, vision, frc, config, lighting, sensors, commonsJVM)
  )

publishArtifact := false

publishMavenStyle := true
publishTo in ThisBuild := Some(Resolver.file("gh-pages-repo", baseDirectory.value / "repo"))
