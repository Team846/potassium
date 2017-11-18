package com.lynbrookrobotics.potassium.commons.electronics

import com.lynbrookrobotics.potassium.streams.Stream
import squants.{Dimensionless, Each, Percent}

sealed trait DitherStatus
case class NotBraking(outputPower: Dimensionless) extends DitherStatus
case class ApplyBrakingPattern(brakingPower: Dimensionless) extends DitherStatus

object DynamicBraking {
  def dynamicBrakingOutput(targetPower: Stream[Dimensionless],
                           currentSpeed: Stream[Dimensionless]): Stream[Option[Dimensionless]] = {
    val ditherStatus: Stream[DitherStatus] = targetPower.zip(currentSpeed).map { case (target, speed) =>
      if ((speed.toPercent < 0) != (target.toPercent < 0)) {
        // change in direction
        NotBraking(target + speed)
      } else if (target.abs < speed.abs) {
        // we are decelerating
        ApplyBrakingPattern(Each(target.abs / speed.abs))
      } else {
        NotBraking(target)
      }
    }

    val brakingPowers = ditherStatus.map {
      case NotBraking(_) => Percent(0)
      case ApplyBrakingPattern(power) => power
    }

    ditherStatus.zip(ditherPattern(brakingPowers)).map {
      case (NotBraking(motorPower), _) => Some(motorPower)
      case (ApplyBrakingPattern(_), true) => None // the pattern tells us to brake on this tick
      case (ApplyBrakingPattern(_), false) => Some(Percent(0)) // the pattern tells us not to brake on this tick
    }
  }

  def ditherPattern(brakingPowers: Stream[Dimensionless]): Stream[Boolean] = {
    brakingPowers.scanLeft((false, 0D, 0D)) { case ((_, total, accum), requestedPower) =>
      // results in a tuple (output, newTotal, newAccum)

      if (requestedPower == Percent(100)) {
        (true, 0D, 0D)
      } else if (requestedPower == Percent(0)) {
        (false, 0D, 0D)
      } else {
        val windowPattern = accum / (total + 1) + (total / (1 + total))

        val (inverted, requestedPowerAfterInvert) = if (requestedPower.toPercent >= 50) {
          (false, requestedPower)
        } else {
          (true, Percent(100) - requestedPower) // now it's greater than 50
        }

        if (windowPattern >= requestedPowerAfterInvert.toEach) {
          // reset if overshoot
          (inverted, 0, (windowPattern - requestedPowerAfterInvert.toEach) * (total + 1))
        } else {
          (!inverted, total + 1, accum)
        }
      }
    }.map(_._1)
  }
}
