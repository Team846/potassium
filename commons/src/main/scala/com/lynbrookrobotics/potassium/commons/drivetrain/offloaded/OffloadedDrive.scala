package com.lynbrookrobotics.potassium.commons.drivetrain.offloaded

import com.lynbrookrobotics.potassium.Signal
import com.lynbrookrobotics.potassium.commons.drivetrain.twoSided.{TwoSided, TwoSidedDrive}
import com.lynbrookrobotics.potassium.control.offload.EscConfig._
import com.lynbrookrobotics.potassium.control.offload.OffloadedSignal
import com.lynbrookrobotics.potassium.control.offload.OffloadedSignal.{OpenLoop, PositionPID, VelocityPIDF}
import com.lynbrookrobotics.potassium.streams.Stream
import squants.space.Length
import squants.{Dimensionless, Velocity}

abstract class OffloadedDrive extends TwoSidedDrive {
  override type DriveSignal = TwoSided[OffloadedSignal]
  override type Properties <: OffloadedProperties

  override def velocityControl(target: Stream[TwoSided[Velocity]])
                              (implicit hardware: Hardware,
                               props: Signal[Properties]): Stream[DriveSignal] = target.map {
    case TwoSided(left, right) =>
      implicit val curProps: Properties = props.get
      implicit val c = curProps.escConfig
      TwoSided(
        VelocityPIDF(
          forwardToAngularVelocityGains(curProps.leftVelocityGainsFull), ticks(left)
        ),
        VelocityPIDF(
          forwardToAngularVelocityGains(curProps.rightVelocityGainsFull), ticks(right)
        )
      )
  }

  def positionControl(target: Stream[TwoSided[Length]])
                     (implicit hardware: Hardware,
                      props: Signal[Properties]): Stream[DriveSignal] = target.map {
    case TwoSided(left, right) =>
      implicit val curProps: Properties = props.get
      implicit val c = curProps.escConfig
      TwoSided(
        PositionPID(
          forwardToAngularPositionGains(curProps.forwardPositionGains), ticks(left)
        ),
        PositionPID(
          forwardToAngularPositionGains(curProps.forwardPositionGains), ticks(right)
        )
      )
  }

  override protected def openLoopToDriveSignal(openLoopInput: TwoSided[Dimensionless]): TwoSided[OffloadedSignal] =
    TwoSided(OpenLoop(openLoopInput.left), OpenLoop(openLoopInput.right))
}