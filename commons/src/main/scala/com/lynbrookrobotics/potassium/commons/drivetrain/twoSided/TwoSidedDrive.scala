package com.lynbrookrobotics.potassium.commons.drivetrain.twoSided

import com.lynbrookrobotics.potassium.Signal
import com.lynbrookrobotics.potassium.commons.drivetrain.unicycle.{UnicycleDrive, UnicycleSignal}
import com.lynbrookrobotics.potassium.control.PIDF
import com.lynbrookrobotics.potassium.streams.Stream
import com.lynbrookrobotics.potassium.units._
import squants.{Dimensionless, Velocity}
import squants.space.Length


/**
  * A drivetrain with two side control (such as a tank drive)
  */
abstract class TwoSidedDrive
  extends UnicycleDrive { self =>

  type DriveSignal = TwoSidedSignal

  type Hardware <: TwoSidedDriveHardware
  type Properties <: TwoSidedDriveProperties

  /**
    * Output the current signal to actuators with the hardware
    *
    * @param hardware the hardware to output with
    * @param signal   the signal to output
    */
  protected def output(hardware: Hardware, signal: TwoSidedSignal): Unit

  protected def convertUnicycleToDrive(uni: UnicycleSignal): TwoSidedSignal = {
    TwoSidedSignal(
      uni.forward + uni.turn,
      uni.forward - uni.turn
    )
  }

  protected def expectedVelocity(drive: TwoSidedSignal)(implicit props: Properties): TwoSidedVelocity = {
    TwoSidedVelocity(
      props.maxForwardVelocity * drive.left,
      props.maxForwardVelocity * drive.right
    )
  }

  protected def driveClosedLoop(signal: Stream[TwoSidedSignal])
                               (implicit hardware: Hardware,
                                props: Signal[Properties]): Stream[TwoSidedSignal] =
    closedLoopControl(signal)

  def velocityControl(target: Stream[TwoSidedVelocity])
                     (implicit hardware: Hardware,
                      props: Signal[Properties]): Stream[TwoSidedSignal] = {
    import hardware._

    val leftControl = PIDF.pidf(
      leftVelocity,
      target.map(_.left),
      props.map(_.leftControlGainsFull)
    )

    val rightControl = PIDF.pidf(
      rightVelocity,
      target.map(_.right),
      props.map(_.rightControlGainsFull)
    )

    leftControl.zip(rightControl).map(s => TwoSidedSignal(s._1, s._2))
  }

  def closedLoopControl(signal: Stream[TwoSidedSignal])
                       (implicit hardware: Hardware,
                        props: Signal[Properties]): Stream[TwoSidedSignal] = {
    velocityControl(signal.map(s => expectedVelocity(s)(props.get)))
  }

  def blendedVelocityControl(arcadeSignal: Stream[UnicycleSignal],
                             curvature: Stream[Ratio[Dimensionless, Length]],
                             targetForwardVelocity: Stream[Velocity])
                            (implicit hardware: Hardware,
                             props: Signal[Properties]): Stream[TwoSidedSignal] = {
    val twoSidedSignal = arcadeSignal.map(convertUnicycleToDrive)
    val targetTankSpeeds = twoSidedSignal.map(expectedVelocity(_)(props.get))

    val blendedVelocities = BlendedDriving.blendedDrive(
      targetTankSpeeds,
      targetForwardVelocity,
      curvature)

    velocityControl(blendedVelocities)
  }
}
