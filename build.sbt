import sbtcrossproject.{crossProject, CrossType}

enablePlugins(GitVersioning)

name := "potassium"

organization in ThisBuild := "com.lynbrookrobotics"

val potassiumVersion = "0.1.0"
val isRelease = sys.props.get("TRAVIS_TAG").isDefined

git.formattedShaVersion := git.gitHeadCommit.value map { sha =>
  if (isRelease) potassiumVersion else s"$potassiumVersion-${sha.take(8)}"
}

scalaVersion in ThisBuild := "2.12.1"

resolvers += "Funky-Repo" at "http://lynbrookrobotics.com/repo"

lazy val sharedDependencies = if (System.getenv("NATIVE_TARGET") == "ARM32") {
  Def.setting(Seq(
    "org.typelevel"  %%% "squants"  % "1.3.0"
  ))
} else {
  Def.setting(Seq(
    "org.typelevel"  %%% "squants"  % "1.3.0",
    "org.scalatest" %%% "scalatest" % "3.1.0-SNAP6" % Test,
    "org.scalacheck" %%% "scalacheck" % "1.13.5" % Test
  ))
}

lazy val jvmDependencies = Seq(
  "org.mockito" % "mockito-core" % "2.3.11" % Test,
  "com.storm-enroute" %% "scalameter-core" % "0.8.2" % Test
)

lazy val potassium = project.in(file(".")).
  aggregate(
    coreJVM, coreJS, coreNative,
    controlJVM, controlJS, controlNative,
    sensorsJVM, sensorsJS, sensorsNative,
    commonsJVM, commonsJS, commonsNative,
    modelJVM, modelNative,
    frcJVM, frcNative,
    remote,
    vision,
    config,
    lighting
  ).settings(
  publish := {},
  publishLocal := {}
)

addCommandAlias(
  "testAll",
  (potassium: ProjectDefinition[ProjectReference])
    .aggregate
    .map(p => s"${p.asInstanceOf[LocalProject].project}/test")
    .mkString(";", ";", "")
)

lazy val nativeSettings = Def.settings(
  scalaVersion := "2.11.12",
  nativeLinkStubs in Test := true,
  coverageExcludedPackages := ".*"
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

lazy val model = crossProject(JVMPlatform, NativePlatform).crossType(CrossType.Pure).dependsOn(core, commons).settings(
  name := "potassium-model",
  libraryDependencies ++= sharedDependencies.value
).nativeSettings(nativeSettings)

lazy val modelJVM = model.jvm
lazy val modelNative = model.native

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

val wpiVersion = "2018.2.2"
val ctreVersion = "5.2.1.1"
lazy val frc = crossProject(JVMPlatform, NativePlatform).crossType(CrossType.Full).dependsOn(core, sensors, control).settings(
  name := "potassium-frc",
  libraryDependencies ++= sharedDependencies.value
).jvmSettings(
  libraryDependencies ++= jvmDependencies,
  resolvers += "WPILib-Maven" at "http://team846.github.io/wpilib-maven",

  libraryDependencies += "edu.wpi.first" % "wpilib" % wpiVersion,
  libraryDependencies += "edu.wpi.first" % "ntcore" % wpiVersion,
  libraryDependencies += "com.ctre" % "phoenix" % ctreVersion
).nativeSettings(
  resolvers += "Funky-Repo" at "http://lynbrookrobotics.com/repo",
  if (System.getenv("NATIVE_TARGET") == "ARM32") {
    Seq(
      libraryDependencies += "com.lynbrookrobotics" %%% "wpilib-scala-native" % "0.1-SNAPSHOT",
      //  libraryDependencies += "com.lynbrookrobotics" % "ntcore" % wpiVersion,
      libraryDependencies += "com.lynbrookrobotics" %%% "phoenix-scala-native" % "0.1-SNAPSHOT"
    )
  } else Seq(
    resolvers += "WPILib-Maven" at "http://team846.github.io/wpilib-maven",

    libraryDependencies += "edu.wpi.first" % "wpilib" % wpiVersion,
    libraryDependencies += "edu.wpi.first" % "ntcore" % wpiVersion,
    libraryDependencies += "com.ctre" % "phoenix" % ctreVersion,
    test := { (compile in Compile).value }
  ),
  nativeSettings
)

lazy val frcJVM = frc.jvm
lazy val frcNative = frc.native

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

lazy val sensors = crossProject(JSPlatform, JVMPlatform, NativePlatform).crossType(CrossType.Pure).dependsOn(core).settings(
  name := "potassium-sensors",
  libraryDependencies ++= sharedDependencies.value
).nativeSettings(nativeSettings)

lazy val sensorsJVM = sensors.jvm
lazy val sensorsJS = sensors.js
lazy val sensorsNative = sensors.native

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
  .dependsOn(coreJVM, modelJVM, controlJVM,
    remote, vision, frcJVM, config, sensorsJVM,
    commonsJVM, lighting)
  .settings(
    autoAPIMappings := true,
    docsMappingsAPIDir := "api",
    addMappingsToSiteDir(mappings in (ScalaUnidoc, packageDoc), docsMappingsAPIDir),
    unidocProjectFilter in (ScalaUnidoc, unidoc) :=
      inProjects(coreJVM, modelJVM, controlJVM,
        remote, vision, frcJVM, config, lighting, sensorsJVM, commonsJVM)
  )

publishArtifact := false

publishMavenStyle := true
publishTo in ThisBuild := Some(Resolver.file("gh-pages-repo", baseDirectory.value / "repo"))
