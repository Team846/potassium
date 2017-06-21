---
layout: docs
title: Getting Started
section: intro
position: 1
---

# Getting Started

*Note: If you come from a FIRST Robotics Team, you may be more interested in [our guide](./frc.html) that explains  setting up build tools for deploying software written with Potassium to your robot and integrating with the the FRC standard libraries*

Getting started with writing software in Potassium is super simple! In your `build.sbt` file, simply add a dependency on the core module, which contains the implementation of the architectural components of Potassium, such as signals and tasks.

```scala
libraryDependencies += "com.lynbrookrobotics" %% "potassium-core" % "0.1.0-{insert Git hash here}"
```
