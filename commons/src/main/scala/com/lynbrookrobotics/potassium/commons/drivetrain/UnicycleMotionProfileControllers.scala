package com.lynbrookrobotics.potassium.commons.drivetrain

import java.lang.Math._

import com.lynbrookrobotics.potassium.{PeriodicSignal, Signal}
import squants.motion._

trait UnicycleMotionProfileControllers[DriveSignal, DrivetrainHardware <: UnicycleHardware, DrivetrainProperties <: UnicycleProperties]
  extends UnicycleCoreControllers[DriveSignal, DrivetrainHardware, DrivetrainProperties] {

  /**
    * This controller uses a control loop that results in constant acceleration,
    * then a constant cruising velocity, and then a constant deceleration to the
    *  target final velocity at the target position. It is meant to control
    *  second order systems.
    * @param initVelocity in feetPerSecond
    * @param cruisingVelocity in feetPerSecond
    * @param finalVelocity in feetPerSecond
    * @param acceleration in FeetPerSecondSquared
    * @param initPosition in Feet
    * @param target  in Feet
    * @param position current position, in Feet
    * @return the velocity to travel at in FeetPerSecond
    */
  def trapezoidalDriveControl(initVelocity: Velocity,
                              cruisingVelocity: Velocity,
                              finalVelocity: Velocity,
                              acceleration: Acceleration,
                              initPosition: Distance,
                              target: Distance,
                              position: Signal[Distance]): PeriodicSignal[Velocity] = {
    val error     = position.map(target - _)
    val signError = error.map(error => Math.signum(error.toFeet))


    /**
      * Calculate the ideal velocity assuming constant acceleration as a
      * function of current position using the equation V^2 = V0^2 + 2a(x-x_0)
      */
    val velocityAccel = position.zip(signError).map { case (pos, sign) =>
      val distanceTraveled = pos - initPosition

      // otherwise additional output is zero and nothing happens
      if (distanceTraveled.toFeet <= 0.5) {
        cruisingVelocity
      } else {
        val V0Squared = Math.pow(initVelocity.toFeetPerSecond, 2)
        val accelerationValue = acceleration.toFeetPerSecondSquared
        val distanceTraveledValue = distanceTraveled.toFeet

        FeetPerSecond(
          math.sqrt(
            math.abs(
              V0Squared + 2 * accelerationValue * distanceTraveledValue)))
      }
    }

    /**
      * Calculate the ideal velocity assuming constant deceleration as a
      * function of current position using the equation V^2 = V0^2 + 2a(x-x_0)
      */
    val velocityDeccel = error.zip(signError).map { case (toTarget, sign) =>
      val finalVelocitySquared = Math.pow(finalVelocity.toFeetPerSecond, 2)
      val errorValue = toTarget.toFeet
      val accelerationValue = acceleration.toFeetPerSecondSquared

      FeetPerSecond(
        sqrt(
          abs(
            finalVelocitySquared + 2 * -accelerationValue * errorValue)))
    }

    velocityDeccel.zip(velocityAccel).zip(signError).map {
      case ((velDec, velAcc), sign) => sign * Utility.min(
        velDec,
        cruisingVelocity,
        velAcc)
    }.toPeriodic
  }
}

object Utility {
  def min(a: Double, b: Double, c: Double): Double = Math.min(a, Math.min(b, c))

  def min(a: Velocity, b: Velocity, c: Velocity): Velocity = {
    a.unit(min(a.toFeetPerSecond, b.toFeetPerSecond, c.toFeetPerSecond))
  }

}