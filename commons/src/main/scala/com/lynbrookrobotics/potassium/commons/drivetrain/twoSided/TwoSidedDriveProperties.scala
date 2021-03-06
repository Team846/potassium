package com.lynbrookrobotics.potassium.commons.drivetrain.twoSided

import com.lynbrookrobotics.potassium.commons.drivetrain.ForwardVelocityGains
import com.lynbrookrobotics.potassium.commons.drivetrain.unicycle.UnicycleProperties
import com.lynbrookrobotics.potassium.control.PIDConfig
import com.lynbrookrobotics.potassium.units._
import squants.motion.{MetersPerSecond, MetersPerSecondSquared}
import squants.space.{Length, Meters}
import squants.{Each, Percent, Velocity}

trait TwoSidedDriveProperties extends UnicycleProperties {
  val maxLeftVelocity: Velocity
  val maxRightVelocity: Velocity

  val leftVelocityGains: ForwardVelocityGains
  val rightVelocityGains: ForwardVelocityGains

  lazy val leftVelocityGainsFull: ForwardVelocityGains#Full =
    leftVelocityGains.withF(Percent(100) / maxLeftVelocity)
  lazy val rightVelocityGainsFull: ForwardVelocityGains#Full =
    rightVelocityGains.withF(Percent(100) / maxRightVelocity)

  lazy val maxForwardVelocity: Velocity = maxLeftVelocity min maxRightVelocity

  val forwardVelocityGains: ForwardVelocityGains = PIDConfig(
    Each(0) / MetersPerSecond(1),
    Each(0) / Meters(1),
    Each(0) / MetersPerSecondSquared(1)
  )

  val track: Length

  val blendExponent: Double
}
