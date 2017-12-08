package com.lynbrookrobotics.potassium.commons.electronics

import com.lynbrookrobotics.potassium.streams.Stream
import squants.{Dimensionless, Each, Percent, Time}
import squants.time.{Milliseconds, TimeDerivative}

object CurrentLimiting {
  def slewRate(lastMotorCommand: Dimensionless,
               input: Stream[Dimensionless],
               slewRate: TimeDerivative[Dimensionless]): Stream[Dimensionless] = {
    def limit(previousOutput: Dimensionless, target: Dimensionless, dt: Time) = {
      if (previousOutput < target) {
        previousOutput + (slewRate * dt)
      } else {
        target
      }
    }

    input.zipWithDt.scanLeft(lastMotorCommand) { case (acc, (target, dt)) =>
      if (target < Percent(0)) {
        -limit(-acc, -target, dt)
      } else {
        limit(acc, target, dt)
      }
    }
  }

  def limitCurrentOutput(targetPower: Dimensionless,
                         normalizedVelocity: Dimensionless,
                         forwardCurrentLimit: Dimensionless,
                         backwardsCurrentLimit: Dimensionless): Dimensionless = {
    if (normalizedVelocity < Each(0)) {
      -limitCurrentOutput(-targetPower, -normalizedVelocity, forwardCurrentLimit, backwardsCurrentLimit)
    } else if (targetPower > normalizedVelocity) {
      targetPower min (normalizedVelocity + forwardCurrentLimit)
    } else if (targetPower < Each(0)){
      val limitedInput = Each(-backwardsCurrentLimit / (Each(1) + normalizedVelocity))
      limitedInput max targetPower
    } else {
      targetPower
    }
  }
}
