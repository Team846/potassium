package com.lynbrookrobotics.potassium.commons.drivetrain.twoSided

import com.lynbrookrobotics.potassium.Signal
import com.lynbrookrobotics.potassium.commons.drivetrain.unicycle.{UnicycleDrive, UnicycleSignal}
import com.lynbrookrobotics.potassium.streams.Stream
import com.lynbrookrobotics.potassium.units._
import squants.space.Length
import squants.{Dimensionless, Velocity}


/**
  * A drivetrain with two side control (such as a tank drive)
  */
abstract class TwoSidedDrive
  extends UnicycleDrive {
  self =>

  type DriveSignal <: TwoSided[_]
  type OpenLoopSignal = TwoSided[Dimensionless]

  type Hardware <: TwoSidedDriveHardware
  type Properties <: TwoSidedDriveProperties

  protected def driveClosedLoop(signal: Stream[OpenLoopSignal])
                               (implicit hardware: Hardware,
                                props: Signal[Properties]): Stream[DriveSignal] =
    velocityControl(signal.map(d =>
      TwoSided[Velocity](
        props.get.maxLeftVelocity * d.left,
        props.get.maxRightVelocity * d.right
      ))
    )

  def velocityControl(target: Stream[TwoSided[Velocity]])
                     (implicit hardware: Hardware,
                      props: Signal[Properties]): Stream[DriveSignal]

  def blendedVelocityControl(arcadeSignal: Stream[UnicycleSignal],
                             curvature: Stream[Ratio[Dimensionless, Length]],
                             targetForwardVelocity: Stream[Velocity])
                            (implicit hardware: Hardware,
                             props: Signal[Properties]): Stream[DriveSignal] = {
    val twoSidedSignal = arcadeSignal.map(unicycleToOpenLoopSignal)
    val targetTankSpeeds = twoSidedSignal.map(expectedVelocity(_)(props.get))

    val blendedVelocities = BlendedDriving.blendedDrive(
      targetTankSpeeds,
      targetForwardVelocity,
      curvature
    )

    velocityControl(blendedVelocities)
  }

  /**
    * Output the current signal to actuators with the hardware
    *
    * @param hardware the hardware to output with
    * @param signal   the signal to output
    */
  protected def output(hardware: Hardware, signal: DriveSignal): Unit

  protected def expectedVelocity(drive: OpenLoopSignal)(implicit props: Properties): TwoSided[Velocity] = {
    TwoSided(
      props.maxForwardVelocity * drive.left,
      props.maxForwardVelocity * drive.right
    )
  }
}
