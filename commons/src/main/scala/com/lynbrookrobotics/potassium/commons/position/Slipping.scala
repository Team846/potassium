package com.lynbrookrobotics.potassium.commons.position

import com.lynbrookrobotics.potassium.streams._
import squants.Velocity
import squants.motion.{Acceleration, AngularVelocity}
import squants.space.{Feet, Length}

object Slipping {
  def slippingDetection (angularVelocity: Stream[AngularVelocity],
                         linearAcceleration: Stream[Acceleration],
                         leftEncoderVelocity: Stream[Velocity],
                         rightEncoderVelocity: Stream[Velocity],
                         distanceFromAccelerometerLeft: Length,
                         distanceFromAccelerometerRight: Length,
                         allowedAccelerationDeviation: Acceleration): Stream[(Boolean, Boolean)] = {
    val radius = linearAcceleration.zip(angularVelocity.derivative).map{ case(linearAcc, angularAcc) =>
      Feet(linearAcc.toFeetPerSecondSquared / angularAcc.toRadiansPerSecondSquared)
    }

    val leftRadius = radius.map(_ - distanceFromAccelerometerLeft)
    val rightRadius = radius.map(_ + distanceFromAccelerometerRight)

    val leftCalculatedAcceleration: Stream[Acceleration] = leftRadius.zip(angularVelocity.derivative).map{ case(radius, angularAcc) =>
      angularAcc onRadius radius
    }
    val rightCalculatedAcceleration = rightRadius.zip(angularVelocity.derivative).map{ case(radius, angularAcc) =>
      angularAcc onRadius radius
    }

    val leftIsSlipping = leftCalculatedAcceleration.zip(leftEncoderVelocity.derivative).map{ case(calculatedAcc, actualAcc) =>
      (calculatedAcc - actualAcc).abs > allowedAccelerationDeviation
    }

    val rightIsSlipping = rightCalculatedAcceleration.zip(rightEncoderVelocity.derivative).map{ case(calculatedAcc, actualAcc) =>
      (calculatedAcc - actualAcc).abs > allowedAccelerationDeviation
    }

    leftIsSlipping.zip(rightIsSlipping)
  }
}
