package com.lynbrookrobotics.potassium.commons.lift.offloaded

import com.lynbrookrobotics.potassium.Signal
import com.lynbrookrobotics.potassium.commons.lift.Lift
import com.lynbrookrobotics.potassium.control.offload.EscConfig.{forwardToAngularPositionGains}
import com.lynbrookrobotics.potassium.control.offload.OffloadedSignal.{OpenLoop, PositionBangBang, PositionPID}
import com.lynbrookrobotics.potassium.control.offload.{EscConfig, OffloadedSignal}
import com.lynbrookrobotics.potassium.streams._
import squants.Dimensionless
import squants.space.Length

abstract class OffloadedLift extends Lift {
  override type LiftSignal = OffloadedSignal
  override type Properties <: OffloadedLiftProperties

  override def positionControl(target: Stream[Length])
                              (implicit properties: Signal[Properties],
                               hardware: Hardware): (Stream[Length], Stream[LiftSignal]) = (
    hardware.
      position.
      zipAsync(target)
      .map(t => t._2 - t._1),

    target.map { it =>
      val curProps = properties.get
      PositionPID(
        forwardToAngularPositionGains(curProps.positionGains)(curProps.escConfig), curProps.toNative(it)
      )
    }
  )

  override def stayAbove(target: Stream[Length])
                        (implicit p: Signal[Properties],
                         hardware: Hardware): Stream[LiftSignal] = target.map { it =>
    PositionBangBang(
      forwardWhenBelow = true, reverseWhenAbove = false,
      signal = p.get.toNative(it)
    )
  }

  override def stayBelow(target: Stream[Length])
                        (implicit p: Signal[Properties],
                         hardware: Hardware): Stream[LiftSignal] = target.map { it =>
    PositionBangBang(
      forwardWhenBelow = false, reverseWhenAbove = true,
      signal = p.get.toNative(it)
    )
  }

  override def openLoopToLiftSignal(x: Dimensionless): LiftSignal = OpenLoop(x)
}

