package com.lynbrookrobotics.potassium.commons.drivetrain

import com.lynbrookrobotics.potassium.streams.Stream
import com.lynbrookrobotics.potassium.{Component, Signal}

/**
  * Represents a drivetrain container with a signal type, default controller, and component
  */
trait Drive {
  type DriveSignal
  type OpenLoopSignal
  type Hardware
  type Properties

  /**
    * Drives with the signal with closed-loop control
    * @param signal the signal to drive with
    * @return
    */
  protected def driveClosedLoop(signal: Stream[OpenLoopSignal])
                               (implicit hardware: Hardware,
                                props: Signal[Properties]): Stream[DriveSignal]

  protected def openLoopToDriveSignal(openLoop:OpenLoopSignal):DriveSignal

  protected def defaultController(implicit hardware: Hardware,
                                  props: Signal[Properties]): Stream[DriveSignal]

  type Drivetrain <: Component[DriveSignal]
}
