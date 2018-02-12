package com.lynbrookrobotics.potassium.commons.drivetrain.unicycle.control

import com.lynbrookrobotics.potassium.streams.Stream
import squants.{Length, Time}
import squants.motion._
import squants.space.Feet

trait UnicycleMotionProfileControllers extends UnicycleCoreControllers {
  /**
    * Uses a control loop that results in constant acceleration,
    * then constant cruising velocity, and then constant deceleration to the
    * target final velocity at the target position. It calculates the
    * velocity required, which is then meant to be acted on by a well tuned
    * velocity controller. This controller is meant to control second order
    * systems such as driving a Length.
    *
    * @param cruisingVelocity desired magnitude of cruising velocity
    * @param finalVelocity desired final velocity when target is reached
    * @param acceleration desired magnitude of acceleration of the system
    * @param position current position
    * @return velocity to travel at to achieve a trapezoidal motion profile.
    *         This value must be used a well tuned velocity controller to result
    *         in correct behaviour
    */
  def trapezoidalDriveControl(cruisingVelocity: Velocity,
                              finalVelocity: Velocity,
                              acceleration: Acceleration,
                              position: Stream[Length],
                              targetPosition: Stream[Length],
                              velocity: Stream[Velocity]): (Stream[Velocity], Stream[Length]) = {
    val error          = targetPosition.minus(position)
    val signError      = error.map(error => Math.signum(error.toFeet))
    val LengthTraveled = position.minus(position.currentValue)

    // Travel at 0.1 ft/s for the first 0.25 feet
    val KickstartLength = Feet(0.25)

    val timeFromStart = position.originTimeStream.get.relativize { (startTime, currentTime) =>
      currentTime - startTime
    }

    /**
      * Calculate the magnitude of ideal velocity assuming constant acceleration
      * as function of current position using equation V^2 = V0^2 + 2a(x-x_0).
      * Direction of ideal velocity is decided later.
      */
    val velocityAccel = LengthTraveled.zip(velocity.currentValue).zip(timeFromStart).map { case ((traveled, initVelocity), timeFromStart) =>
      // otherwise additional output is zero and nothing happens
      if (traveled.abs <= KickstartLength) {
        initVelocity + acceleration * timeFromStart
      } else {
        val V0Squared             = Math.pow(initVelocity.toFeetPerSecond, 2)
        val accelerationValue     = acceleration.toFeetPerSecondSquared
        val LengthTraveledValue = traveled.abs.toFeet

        FeetPerSecond(
          math.sqrt(
            math.abs(
              V0Squared + 2 * accelerationValue * LengthTraveledValue)))
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
      val finalVelocitySquared = math.pow(finalVelocity.toFeetPerSecond, 2)
      val errorValue = toTarget.abs.toFeet
      val accelerationValue = acceleration.toFeetPerSecondSquared

      FeetPerSecond(
        math.sqrt(
          math.abs(
            finalVelocitySquared + 2 * accelerationValue * errorValue)))
    }

    // Ensure that motion is in the direction of the error
    val velocityOutput = velocityDeccel.zip(velocityAccel).zip(signError).map {
      case ((velDec, velAcc), sign) => {
        sign * velDec min cruisingVelocity min velAcc
      }
    }

    (velocityOutput, error)
  }

}