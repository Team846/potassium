package com.lynbrookrobotics.potassium.commons.drivetrain

import com.lynbrookrobotics.potassium.{Component, PeriodicSignal, SignalLike}

/**
  * Represents a drivetrain container with a signal type, default controller, and component
  */
trait Drive {
  type DriveSignal
  type DriveVelocity

  protected def driveClosedLoop[C[_]](drive: SignalLike[DriveSignal, C]): PeriodicSignal[DriveSignal]
  protected def driveVelocityControl[C[_]](signal: SignalLike[DriveVelocity, C]): PeriodicSignal[DriveSignal]

  protected def defaultController: PeriodicSignal[DriveSignal]

  type Drivetrain <: Component[DriveSignal]
}
