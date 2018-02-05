package com.lynbrookrobotics.potassium.commons.lift.offloaded

import com.lynbrookrobotics.potassium.Signal
import com.lynbrookrobotics.potassium.commons.lift.Lift
import com.lynbrookrobotics.potassium.control.offload.EscConfig._
import com.lynbrookrobotics.potassium.control.offload.OffloadedSignal
import com.lynbrookrobotics.potassium.control.offload.OffloadedSignal.{OpenLoop, PositionBangBang, PositionPID}
import com.lynbrookrobotics.potassium.streams._
import squants.Dimensionless
import squants.space.Length

abstract class OffloadedLift extends Lift {
  override type LiftSignal = OffloadedSignal
  override type Properties <: OffloadedProperties

  override def positionControl(target: Stream[Length])
                              (implicit properties: Signal[Properties],
                               hardware: Hardware): (Stream[Length], Stream[LiftSignal]) = (
    hardware.
      position.
      zipAsync(target)
      .map(t => t._2 - t._1),

    target.map { it =>
      val curProps = properties.get
      implicit val c = curProps.escConfig
      PositionPID(
        forwardToAngularPositionGains(curProps.positionGains), ticks(it)
      )
    }
  )

  override def stayAbove(target: Stream[Length])
                        (implicit properties: Signal[Properties],
                         hardware: Hardware): (Stream[Length], Stream[LiftSignal]) = (
    hardware.
      position.
      zipAsync(target)
      .map(t => t._2 - t._1),

    target.map { it =>
      implicit val c = properties.get.escConfig
      PositionBangBang(
        forwardWhenBelow = true, reverseWhenAbove = false,
        signal = ticks(it)
      )
    }
  )

  override def stayBelow(target: Stream[Length])
                        (implicit properties: Signal[Properties],
                         hardware: Hardware): (Stream[Length], Stream[LiftSignal]) = (
    hardware.
      position.
      zipAsync(target)
      .map(t => t._2 - t._1),

    target.map { it =>
      implicit val c = properties.get.escConfig
      PositionBangBang(
        forwardWhenBelow = false, reverseWhenAbove = true,
        signal = ticks(it)
      )
    }
  )

  override def openLoopToLiftSignal(x: Dimensionless): LiftSignal = OpenLoop(x)
}

