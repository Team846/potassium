package com.lynbrookrobotics.potassium.commons.drivetrain.twoSided

import com.lynbrookrobotics.potassium.commons.drivetrain.unicycle.UnicycleHardware
import com.lynbrookrobotics.potassium.streams.Stream
import squants.motion.{AngularVelocity, RadiansPerSecond}
import squants.space.Radians
import squants.time.Seconds
import squants.{Angle, Length, Velocity}

trait TwoSidedDriveHardware extends UnicycleHardware {
  val leftVelocity: Stream[Velocity]
  val rightVelocity: Stream[Velocity]

  val leftPosition: Stream[Length]
  val rightPosition: Stream[Length]

  val track: Length

  lazy val forwardVelocity: Stream[Velocity] =
    leftVelocity.zip(rightVelocity).map(t => (t._1 + t._2) / 2)

  lazy val turnVelocity: Stream[AngularVelocity] = {
    rightVelocity.zip(leftVelocity).map {
      case (r, l) =>
        RadiansPerSecond(((l - r) * Seconds(1)) / track)
    }
  }

  lazy val forwardPosition: Stream[Length] =
    leftPosition.zip(rightPosition).map(t => (t._1 + t._2) / 2)

  lazy val turnPosition: Stream[Angle] = leftPosition.zip(rightPosition).map(t => Radians((t._1 - t._2) / track))
}
