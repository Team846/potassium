package com.lynbrookrobotics.potassium.config

import java.io.File

import squants.motion.{MetersPerSecond, Velocity}
import SquantsPickling._

object Test extends App{
  case class RobotConfig(drive: DriveConfig)
  case class DriveConfig(maxForwardSpeed: Velocity)

  val a = TwoWayFileJSON[RobotConfig](new File("test.txt"))

  a.value = RobotConfig(DriveConfig(MetersPerSecond(5.0)))

  val driveConfig = a.map(_.drive)((robot, newDrive) => robot.copy(drive = newDrive))

  val maxFwdSpeed = driveConfig.map(_.maxForwardSpeed)((drive, newForward) => drive.copy(maxForwardSpeed = newForward))

  maxFwdSpeed.value = MetersPerSecond(16.0)

  while (true) {
    println(maxFwdSpeed.value.toMetersPerSecond)
    Thread.sleep(1000)
  }
}
