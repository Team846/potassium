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

  val forwardVelocityGains: ForwardVelocityGains
  val turnVelocityGains: TurnVelocityGains

  lazy val forwardVelocityGainsFull: ForwardVelocityGains#Full =
    forwardVelocityGains.withF(Percent(100) / maxForwardVelocity)

  lazy val turnVelocityGainsFull: TurnVelocityGains#Full =
    turnVelocityGains.withF(Percent(100) / maxTurnVelocity)

  val forwardPositionGains: ForwardPositionGains
  val turnPositionGains: TurnPositionGains
}
