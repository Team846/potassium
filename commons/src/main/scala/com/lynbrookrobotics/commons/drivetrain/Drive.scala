package com.lynbrookrobotics.commons.drivetrain

import com.lynbrookrobotics.potassium.{Component, PeriodicSignal}

/**
  * Represents a drivetrain container with a signal type, default controller, and component
  */
trait Drive {
  type DriveSignal
  type DriveVelocity

  protected def driveClosedLoop(drive: PeriodicSignal[DriveSignal]): PeriodicSignal[DriveSignal]
  protected def driveVelocityControl(signal: PeriodicSignal[DriveVelocity]): PeriodicSignal[DriveSignal]

  protected def defaultController: PeriodicSignal[DriveSignal]

  type Drivetrain <: Component[DriveSignal]
}
