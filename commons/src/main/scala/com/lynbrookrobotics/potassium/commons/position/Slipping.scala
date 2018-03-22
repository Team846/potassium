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
    angularVelocity.derivative.foreach(println(_))
    val calculatedAccelerations = linearAcceleration.zip(angularVelocity.derivative).map{ case(linearAcc, angularAcc) =>
      if (angularAcc.toRadiansPerSecondSquared == 0) {
        println("is zero")
        (linearAcc, linearAcc)
      } else {
        println("is not zero")
        println("linearAcc: " + linearAcc)
        println("angularAcc: " + angularAcc)
        val radius = Feet(linearAcc.toFeetPerSecondSquared / angularAcc.toRadiansPerSecondSquared)
        println("radius: " + radius)
        val leftRadius = radius - distanceFromAccelerometerLeft
        println("leftRadius: " + leftRadius)
        val rightRadius = radius + distanceFromAccelerometerRight
        println("rightRadius: " + rightRadius)

        val leftCalculatedAcceleration = angularAcc onRadius leftRadius
        println("leftCalculatedAcceleration: " + leftCalculatedAcceleration)
        val rightCalculatedAcceleration = angularAcc onRadius rightRadius
        println("rightCalculatedAcceleration: " + rightCalculatedAcceleration)

        (leftCalculatedAcceleration, rightCalculatedAcceleration)
      }
    }

    //radius.foreach(x => System.out.println("calculated radius: " + x))

    val leftIsSlipping = calculatedAccelerations.zip(leftEncoderVelocity.derivative)map { case ((leftAcc, _), actualAcc) =>
      (leftAcc - actualAcc).abs > allowedAccelerationDeviation
    }

    val rightIsSlipping = calculatedAccelerations.zip(leftEncoderVelocity.derivative)map { case ((_, rightAcc), actualAcc) =>
      (rightAcc - actualAcc).abs > allowedAccelerationDeviation
    }

    leftIsSlipping.zip(rightIsSlipping)
  }
}
