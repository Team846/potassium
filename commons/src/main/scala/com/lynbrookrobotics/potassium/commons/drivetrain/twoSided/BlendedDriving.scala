package com.lynbrookrobotics.potassium.commons.drivetrain.twoSided

import com.lynbrookrobotics.potassium.Signal
import com.lynbrookrobotics.potassium.streams._
import com.lynbrookrobotics.potassium.units.Ratio
import squants.motion.RadiansPerSecond
import squants.{Dimensionless, Each, Length, Percent, Velocity}

object BlendedDriving {
  def driveWithRadius(radiusStream: Stream[Length],
                      velocityStream: Stream[Velocity])
                     (implicit props: Signal[TwoSidedDriveProperties]): Stream[TwoSided[Velocity]] = {
    velocityStream.zip(radiusStream).map { case (velocity, radius) =>
      if (radius.value == Double.PositiveInfinity || radius.value == Double.NegativeInfinity || radius.value == Double.NaN) {
        TwoSided(velocity, velocity)
      } else {
        val angularVelocity = RadiansPerSecond(velocity.toFeetPerSecond / radius.toFeet)

        val left = angularVelocity onRadius (radius + props.get.track / 2)
        val right = angularVelocity onRadius (radius - props.get.track / 2)
        TwoSided(left, right)
      }
    }
  }

  /**
    * Take the weighted average of the speed for tank like driving and for
    * driving at a constant radius. The weight of the average is decided by
    * the current, normalized speed
    */
  def blend(constantRadiusSpeed: Velocity,
            tankSpeed: Velocity,
            targetForward: Velocity)
           (implicit props: Signal[TwoSidedDriveProperties]): Velocity = {
    val normalizedTargetForwardSpeed = targetForward / props.get.maxForwardVelocity

    val constantRadiusWeight = Each(
      math.pow(normalizedTargetForwardSpeed.abs, props.get.blendExponent)
    )
    val tankWeight = Percent(100) - constantRadiusWeight

    tankWeight.toEach * tankSpeed + constantRadiusWeight.toEach * constantRadiusSpeed
  }

  def blendedDrive(arcadeSpeed: Stream[TwoSided[Velocity]],
                   targetForwardVelocity: Stream[Velocity],
                   curvature: Stream[Ratio[Dimensionless, Length]])
                  (implicit properties: Signal[TwoSidedDriveProperties]): Stream[TwoSided[Velocity]] = {
    val constantRadiusSpeed = driveWithRadius(radiusStream = curvature.map(curvature => curvature.den / curvature.num.toEach), targetForwardVelocity)

    arcadeSpeed.zip(constantRadiusSpeed).zip(targetForwardVelocity).map {
      case ((tankSpeed, carSpeed), targetForward) =>
        TwoSided(
          blend(carSpeed.left, tankSpeed.left, targetForward),
          blend(carSpeed.right, tankSpeed.right, targetForward))
    }
  }
}
