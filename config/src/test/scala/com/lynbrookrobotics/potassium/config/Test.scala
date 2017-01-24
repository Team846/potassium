package com.lynbrookrobotics.potassium.config

import java.io.File

object Test extends App{
  case class RobotConfig(drive: DriveConfig)
  case class DriveConfig(maxForwardSpeed: Double)

  val a = TwoWayFileJSON[RobotConfig](new File("test.txt"))

  a.value = RobotConfig(DriveConfig(10.0))

  val driveConfig = a.map(_.drive, (robot, newDrive: DriveConfig) => robot.copy(drive = newDrive))

  val maxFwdSpeed = driveConfig.map(_.maxForwardSpeed, (drive, newForward: Double) => drive.copy(maxForwardSpeed = newForward))

  maxFwdSpeed.value = 16.0
}
