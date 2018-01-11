package com.lynbrookrobotics.potassium.commons.drivetrain.twoSided

import com.lynbrookrobotics.potassium.Signal
import com.lynbrookrobotics.potassium.commons.drivetrain.unicycle.{UnicycleDrive, UnicycleSignal}
import com.lynbrookrobotics.potassium.streams.Stream
import com.lynbrookrobotics.potassium.units._
import squants.{Dimensionless, Velocity}


/**
  * A drivetrain with two side control (such as a tank drive)
  */
abstract class TwoSidedDrive extends UnicycleDrive {
  self =>

  override type DriveSignal <: TwoSided[_]

  override type Hardware <: TwoSidedDriveHardware
  override type Properties <: TwoSidedDriveProperties


  override type OpenLoopInput = TwoSided[Dimensionless]

  override protected def driveClosedLoop(signal: Stream[OpenLoopInput])
                                        (implicit hardware: Hardware,
                                         props: Signal[Properties]): Stream[DriveSignal] = {
    velocityControl(signal.map(d =>
      TwoSided[Velocity](
        props.get.maxLeftVelocity * d.left,
        props.get.maxRightVelocity * d.right
      )
    ))
  }

  override protected def convertUnicycleToOpenLoopInput(uni: UnicycleSignal): OpenLoopInput = {
    TwoSided(
      uni.forward + uni.turn,
      uni.forward - uni.turn
    )
  }

  /**
    * Output the current signal to actuators with the hardware
    *
    * @param hardware the hardware to output with
    * @param signal   the signal to output
    */
  protected def output(hardware: Hardware, signal: DriveSignal): Unit

  protected def expectedVelocity(drive: TwoSided[Dimensionless])(implicit props: Properties): TwoSided[Velocity] = {
    TwoSided(
      props.maxForwardVelocity * drive.left,
      props.maxForwardVelocity * drive.right
    )
  }

  def velocityControl(target: Stream[TwoSided[Velocity]])
                     (implicit hardware: Hardware,
                      props: Signal[Properties]): Stream[DriveSignal]
}
