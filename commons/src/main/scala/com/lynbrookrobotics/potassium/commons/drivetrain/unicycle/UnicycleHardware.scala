package com.lynbrookrobotics.potassium.commons.drivetrain.unicycle

import com.lynbrookrobotics.potassium.streams.Stream
import squants.motion.AngularVelocity
import squants.{Angle, Length, Velocity}

trait UnicycleHardware {
  val forwardVelocity: Stream[Velocity]
  val turnVelocity: Stream[AngularVelocity]

  val forwardPosition: Stream[Length]
  val turnPosition: Stream[Angle]
}
