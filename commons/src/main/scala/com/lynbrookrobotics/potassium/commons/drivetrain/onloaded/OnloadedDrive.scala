package com.lynbrookrobotics.potassium.commons.drivetrain.onloaded

import com.lynbrookrobotics.potassium.Signal
import com.lynbrookrobotics.potassium.commons.drivetrain.twoSided.{TwoSided, TwoSidedDrive}
import com.lynbrookrobotics.potassium.commons.drivetrain.unicycle.UnicycleSignal
import com.lynbrookrobotics.potassium.control.PIDF
import com.lynbrookrobotics.potassium.streams.Stream
import com.lynbrookrobotics.potassium.units._
import squants.{Dimensionless, Velocity}

abstract class OnloadedDrive extends TwoSidedDrive {

  type DriveSignal = TwoSided[Dimensionless]

  override def openLoopToDriveSignal(openLoopInput: TwoSided[Dimensionless]): TwoSided[Dimensionless] = openLoopInput

  def velocityControl(target: Stream[TwoSided[Velocity]])
                     (implicit hardware: Hardware,
                      props: Signal[Properties]): Stream[DriveSignal] = {
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

  override protected def convertUnicycleToOpenLoopInput(uni: UnicycleSignal): TwoSided[Dimensionless] = {
    TwoSided(
      uni.forward + uni.turn,
      uni.forward - uni.turn
    )
  }
}
