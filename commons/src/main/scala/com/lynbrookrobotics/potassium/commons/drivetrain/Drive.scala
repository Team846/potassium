package com.lynbrookrobotics.potassium.commons.drivetrain

import com.lynbrookrobotics.potassium.commons.drivetrain.twoSided.TwoSided
import com.lynbrookrobotics.potassium.streams.Stream
import com.lynbrookrobotics.potassium.{Component, Signal}
import squants.Dimensionless

/**
  * Represents a drivetrain container with a signal type, default controller, and component
  */
trait Drive {
  type Drivetrain <: Component[DriveSignal]
  type DriveSignal
  type OpenLoopInput
  type Hardware
  type Properties

  /**
    * Drives with the signal with closed-loop control
    *
    * @param signal the signal to drive with
    * @return
    */
  protected def driveClosedLoop(signal: Stream[OpenLoopInput])
                               (implicit hardware: Hardware,
                                props: Signal[Properties]): Stream[DriveSignal]

  protected def openLoopToDriveSignal(openLoopInput: OpenLoopInput): DriveSignal

  protected def defaultController(implicit hardware: Hardware,
                                  props: Signal[Properties]): Stream[DriveSignal]
}
