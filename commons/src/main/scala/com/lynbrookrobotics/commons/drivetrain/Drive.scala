package com.lynbrookrobotics.commons.drivetrain

import com.lynbrookrobotics.potassium.{Component, PeriodicSignal, Signal}

/**
  * Represents a drivetrain container with a signal type, default controller, and component
  */
trait Drive {
  type DriveSignal
  type DriveVelocity

  protected def driveClosedLoop(drive: Signal[DriveSignal]): Signal[DriveSignal]
  protected def driveVelocityControl(signal: Signal[DriveVelocity]): Signal[DriveSignal]

  protected def defaultController: PeriodicSignal[DriveSignal]

  type Drivetrain <: Component[DriveSignal]
}
