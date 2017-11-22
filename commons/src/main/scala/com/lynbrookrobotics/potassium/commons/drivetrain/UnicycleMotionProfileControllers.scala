package com.lynbrookrobotics.potassium.commons.drivetrain

import com.lynbrookrobotics.potassium.Signal
import com.lynbrookrobotics.potassium.control.{PIDFProperUnitsConfig, TrapezoidalControl, TrapezoidalProfileConfig}
import com.lynbrookrobotics.potassium.streams.Stream
import squants.{Dimensionless, Percent, Quantity}
import squants.motion._
import squants.space.{Feet, Length}
import squants.time.{TimeDerivative, TimeIntegral}

trait UnicycleMotionProfileControllers extends UnicycleCoreControllers {

  // Travel at 0.1 ft/s for the first 0.25 feet
  val KickstartDistance = Feet(0.25)
  val KickStartVelocity = FeetPerSecond(1)

  type profileConfig = TrapezoidalProfileConfig[Velocity, Acceleration, Length]
  /**
    * Uses a control loop that results in constant acceleration,
    * then constant cruising velocity, and then constant deceleration to the
    * target final velocity at the target position. It calculates the
    * velocity required, which is then meant to be acted on by a well tuned
    * velocity controller. This controller is meant to control second order
    * systems such as driving a distance.
    *
    * @param cruisingVelocity desired magnitude of cruising velocity
    * @param finalVelocity desired final velocity when target is reached
    * @param acceleration desired magnitude of acceleration of the system
    * @param targetForwardTravel target position
    * @param position current position
    * @return velocity to travel at to achieve a trapezoidal motion profile.
    *         This value must be used a well tuned velocity controller to result
    *         in correct behaviour
    */
  def trapezoidalDriveControl(cruisingVelocity: Velocity,
                              finalVelocity: Velocity,
                              acceleration: Acceleration,
                              targetForwardTravel: Length,
                              position: Stream[Length],
                              velocity: Stream[Velocity],
                              config: Signal[profileConfig]): (Stream[UnicycleSignal], Stream[Length]) = {
    val (out, error) = TrapezoidalControl.trapezoidalControl[Velocity, Acceleration, Length](
      cruisingVelocity,
      finalVelocity,
      acceleration,
      targetForwardTravel,
      position,
      velocity,
      config)

    (out.map(UnicycleSignal(_, Percent(0))), error)
  }

}