package com.lynbrookrobotics.potassium.commons.drivetrain.unicycle

import com.lynbrookrobotics.potassium.Signal
import squants.motion.AngularVelocity
import squants.{Dimensionless, Each, Velocity}

case class UnicycleSignal(forward: Dimensionless, turn: Dimensionless) {
  def +(that: UnicycleSignal): UnicycleSignal =
    UnicycleSignal(this.forward + that.forward, this.turn + that.turn)
}

case class UnicycleVelocity(forward: Velocity, turn: AngularVelocity) {
  def toUnicycleSignal(implicit unicycleProperties: Signal[UnicycleProperties]) = {
    UnicycleSignal(
      Each(forward / unicycleProperties.get.maxForwardVelocity),
      Each(turn / unicycleProperties.get.maxTurnVelocity))
  }
}
