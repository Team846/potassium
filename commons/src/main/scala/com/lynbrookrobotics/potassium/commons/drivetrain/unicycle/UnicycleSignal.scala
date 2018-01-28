package com.lynbrookrobotics.potassium.commons.drivetrain.unicycle

import com.lynbrookrobotics.potassium.Signal
import squants.motion.AngularVelocity
import squants.{Dimensionless, Each, Velocity}

case class UnicycleSignal(forward: Dimensionless, turn: Dimensionless) {
  def +(that: UnicycleSignal): UnicycleSignal =
    UnicycleSignal(this.forward + that.forward, this.turn + that.turn)

  def toUnicycleVelocity(implicit p: Signal[UnicycleProperties]): UnicycleVelocity = {
    val curProps = p.get
    UnicycleVelocity(
      curProps.maxForwardVelocity * forward.toEach,
      curProps.maxTurnVelocity * turn.toEach
    )
  }
}

case class UnicycleVelocity(forward: Velocity, turn: AngularVelocity) {
  def toUnicycleSignal(implicit p: Signal[UnicycleProperties]): UnicycleSignal = {
    val curProps = p.get
    UnicycleSignal(
      Each(forward / curProps.maxForwardVelocity),
      Each(turn / curProps.maxTurnVelocity))
  }
}
