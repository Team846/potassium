package com.lynbrookrobotics.potassium.commons.drivetrain

import com.lynbrookrobotics.potassium.streams.Stream

import squants.motion._
import squants.space.Feet

trait UnicycleMotionProfileControllers extends UnicycleCoreControllers {
  /**
    * Uses a control loop that results in constant acceleration,
    * then constant cruising velocity, and then constant deceleration to the
    * target final velocity at the target position. It calculates the
    * velocity required, which is then meant to be acted on by a well tuned
    * velocity controller. This controller is meant to control second order
    * systems such as driving a distance.
    *
    * @param initVelocity initial velocity of the system
    * @param cruisingVelocity desired magnitude of cruising velocity
    * @param finalVelocity desired final velocity when target is reached
    * @param acceleration desired magnitude of acceleration of the system
    * @param initPosition initial position of the system
    * @param targetForwardTravel target position
    * @param position current position
    * @return velocity to travel at to achieve a trapezoidal motion profile.
    *         This value must be used a well tuned velocity controller to result
    *         in correct behaviour
    */
  def trapezoidalDriveControl(initVelocity: Velocity,
                              cruisingVelocity: Velocity,
                              finalVelocity: Velocity,
                              acceleration: Acceleration,
                              initPosition: Distance,
                              targetForwardTravel: Distance,
                              position: Stream[Distance],
                              tolerance: Distance): (Stream[Velocity], Stream[Distance]) = {
    val targetPosition   = initPosition + targetForwardTravel
    val error            = position.map(targetPosition - _)
    val signError        = error.map(error => Math.signum(error.toFeet))
    val distanceTraveled = position.map(_ - initPosition)

    // Travel at 0.1 ft/s for the first 0.25 feet
    val KickstartDistance = Feet(0.25)
    val Tolerance         = Feet(0.1)
    val KickStartVelocity = FeetPerSecond(1)

    /**
      * Calculate the magnitude of ideal velocity assuming constant acceleration
      * as function of current position using equation V^2 = V0^2 + 2a(x-x_0).
      * Direction of ideal velocity is decided later.
      */
    val velocityAccel = distanceTraveled.map { traveled =>
      // otherwise additional output is zero and nothing happens
      if (traveled.abs <= KickstartDistance) {
        KickStartVelocity
      } else {
        val V0Squared             = Math.pow(initVelocity.toFeetPerSecond, 2)
        val accelerationValue     = acceleration.toFeetPerSecondSquared
        val distanceTraveledValue = traveled.abs.toFeet

        FeetPerSecond(
          math.sqrt(
            math.abs(
              V0Squared + 2 * accelerationValue * distanceTraveledValue)))
      }
    }

    /**
      * Calculate magnitude of ideal velocity assuming constant deceleration
      * using the equation V_final^2 = V_curr^2 + 2a(target - x_current).
      * Solving for V_curr, we get V_curr^2 = V_f^2 - 2a(target - x_curr).
      * Since, we are decelerating, a = -acceleration (value passed on
      * construction),  getting
      * V_curr = V_curr^2 = V_f^2 + 2*acceleration*(target - x_curr)
      * Direction of ideal velocity is decided later.
      */
    val velocityDeccel = error.map { toTarget =>
      if (toTarget.abs <= tolerance) {
        FeetPerSecond(0.0)
      } else {
        val finalVelocitySquared = Math.pow(finalVelocity.toFeetPerSecond, 2)
        val errorValue = toTarget.abs.toFeet
        val accelerationValue = acceleration.toFeetPerSecondSquared

        FeetPerSecond(
          math.sqrt(
            math.abs(
              finalVelocitySquared + 2 * accelerationValue * errorValue)))
      }
    }

    // Ensure that motion is in the direction of the error
    val velocityOutput = velocityDeccel.zip(velocityAccel).zip(signError).map {
      case ((velDec, velAcc), sign) => sign * velDec min cruisingVelocity min velAcc
    }

    (velocityOutput, error)
  }

}