package com.lynbrookrobotics.potassium.commons.electronics

import com.lynbrookrobotics.potassium.streams.Stream
import squants.{Dimensionless, Percent}
import squants.time.{Milliseconds, TimeDerivative}
import squants.Time

object CurrentLimiting {
  def slewRate(input: Stream[Dimensionless],
               slewRate: TimeDerivative[Dimensionless]): Stream[Dimensionless] = {
    def limit(previousOutput: Dimensionless, target: Dimensionless, dt: Time) = {
      if (previousOutput < target) {
        previousOutput + (slewRate * dt)
      } else {
        target
      }
    }

    // TODO: Fix
    input.zipWithDt(null).scanLeft(Percent(0)) { case (acc, (target, dt)) =>
      if (target < Percent(0)) {
        -limit(-acc, -target, dt)
      } else {
        limit(acc, target, dt)
      }
    }
  }
}
