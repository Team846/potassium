package com.lynbrookrobotics.commons.drivetrain

import com.lynbrookrobotics.potassium.{Component, PeriodicSignal, Signal}

/**
  * Represents a drivetrain container with a signal type, default controller, and component
  */
trait Drive {
  type DriveSignal
  type DriveVelocity

  protected def driveExpectedVelocity(drive: DriveSignal): DriveVelocity
  protected def driveVelocityControl(signal: Signal[DriveVelocity]): Signal[DriveSignal]

  protected val defaultController: PeriodicSignal[DriveSignal]

  type Drivetrain <: Component[DriveSignal]
}
