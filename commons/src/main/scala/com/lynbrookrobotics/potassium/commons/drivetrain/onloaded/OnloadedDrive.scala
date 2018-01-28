package com.lynbrookrobotics.potassium.commons.drivetrain.onloaded

import com.lynbrookrobotics.potassium.Signal
import com.lynbrookrobotics.potassium.commons.drivetrain.twoSided.{TwoSided, TwoSidedDrive}
import com.lynbrookrobotics.potassium.commons.drivetrain.unicycle.UnicycleSignal
import com.lynbrookrobotics.potassium.control.PIDF
import com.lynbrookrobotics.potassium.streams._
import squants.{Dimensionless, Velocity}

abstract class OnloadedDrive extends TwoSidedDrive {
  type DriveSignal = TwoSided[Dimensionless]
  override type OpenLoopSignal = TwoSided[Dimensionless]

  override def velocityControl(target: Stream[TwoSided[Velocity]])
                              (implicit hardware: Hardware, props: Signal[Properties]): Stream[TwoSided[Dimensionless]] = {
    import hardware._
    val leftControl = PIDF.pidf(
      leftVelocity,
      target.map(_.left),
      props.map(_.leftVelocityGainsFull)
    )

    val rightControl = PIDF.pidf(
      rightVelocity,
      target.map(_.right),
      props.map(_.rightVelocityGainsFull)
    )

    leftControl.zip(rightControl).map(s => TwoSided(s._1, s._2))
  }

  /**
    * Converts a unicycle signal value to the parent's signal type
    *
    * @param uni the unicycle value to convert
    * @return the parent's equivalent signal
    */
  override protected def unicycleToOpenLoopSignal(uni: UnicycleSignal): TwoSided[Dimensionless] =
    TwoSided(
      uni.forward + uni.turn,
      uni.forward - uni.turn
    )

  override protected def openLoopToDriveSignal(openLoop: TwoSided[Dimensionless]) = openLoop
}