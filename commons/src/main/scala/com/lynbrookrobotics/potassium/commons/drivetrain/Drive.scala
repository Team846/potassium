package com.lynbrookrobotics.potassium.commons.drivetrain

import com.lynbrookrobotics.potassium.{Component, PeriodicSignal, Signal, SignalLike}

/**
  * Represents a drivetrain container with a signal type, default controller, and component
  */
trait Drive {
  type DriveSignal
  type DriveVelocity

  type DrivetrainHardware
  type DrivetrainProperties

  /**
    * Drives with the signal with closed-loop control
    * @param signal the signal to drive with
    * @return
    */
  protected def driveClosedLoop(signal: SignalLike[DriveSignal])(implicit hardware: DrivetrainHardware,
                                                                 props: Signal[DrivetrainProperties]): PeriodicSignal[DriveSignal]

  protected def defaultController(implicit hardware: DrivetrainHardware,
                                  props: Signal[DrivetrainProperties]): PeriodicSignal[DriveSignal]

  type Drivetrain <: Component[DriveSignal]
}
