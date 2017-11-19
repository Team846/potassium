package com.lynbrookrobotics.potassium.commons.electronics

import com.lynbrookrobotics.potassium.streams.Stream
import squants.{Dimensionless, Each, Percent, Time}
import squants.time.{Milliseconds, TimeDerivative}

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

    input.zipWithDt.scanLeft(Percent(0)) { case (acc, (target, dt)) =>
      if (target < Percent(0)) {
        -limit(-acc, -target, dt)
      } else {
        limit(acc, target, dt)
      }
    }
  }

  def limitCurrentOutput(input: Dimensionless,
                         normalizedV: Dimensionless,
                         forwardCurrentLimit: Dimensionless,
                         backwardsCurrentLimit: Dimensionless): Dimensionless = {
    if(normalizedV < Each(0)) {
      -limitCurrentOutput(-input, -normalizedV, forwardCurrentLimit, backwardsCurrentLimit)
    }
    if(input > normalizedV){
      input.min(normalizedV + forwardCurrentLimit)
    } else if(input < Each(0)){
      val limitedInput = Each(-backwardsCurrentLimit / (Each(1) + normalizedV))
      limitedInput.max(input)
    } else {
      input
    }
  }
}
