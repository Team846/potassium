package com.lynbrookrobotics.potassium.commons.drivetrain.unicycle

import com.lynbrookrobotics.potassium.commons.drivetrain.{ForwardPositionGains, ForwardVelocityGains, TurnPositionGains, TurnVelocityGains}
import com.lynbrookrobotics.potassium.units._
import squants.motion.AngularVelocity
import squants.{Acceleration, Length, Percent, Velocity}

trait UnicycleProperties {
  val maxForwardVelocity: Velocity
  val maxTurnVelocity: AngularVelocity
  val maxAcceleration: Acceleration
  val defaultLookAheadDistance: Length

  val forwardControlGains: ForwardVelocityGains

  lazy val forwardControlGainsFull: ForwardVelocityGains#Full = {
    forwardControlGains.withF(Percent(100) / maxForwardVelocity)
  }

  val turnControlGains: TurnVelocityGains

  lazy val turnControlGainsFull: TurnVelocityGains#Full = {
    turnControlGains.withF(Percent(100) / maxTurnVelocity)
  }

  val forwardPositionControlGains: ForwardPositionGains

  val turnPositionControlGains: TurnPositionGains
}
