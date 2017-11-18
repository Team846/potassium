package com.lynbrookrobotics.potassium.commons.electronics

import com.lynbrookrobotics.potassium.streams.Stream
import squants.{Dimensionless, Each, Percent}

sealed trait DitherStatus {
  def negate: DitherStatus
}

case class NotBraking(outputPower: Dimensionless) extends DitherStatus {
  override def negate: DitherStatus = NotBraking(-outputPower)
}

case class ApplyBrakingPattern(brakingPower: Dimensionless) extends DitherStatus {
  override def negate: DitherStatus = this
}

object DynamicBraking {
  private def calculateBrakeAndDutyCycle(desired: Dimensionless, currentSpeed: Dimensionless): DitherStatus = {
    if (currentSpeed < Percent(0)) {
      calculateBrakeAndDutyCycle(-desired, -currentSpeed).negate
    } else {
      // speed >= 0 at this point
      if (desired >= currentSpeed) { // going faster
        NotBraking(desired)
      } else { // slowing down
        val error = desired - currentSpeed // error always <= 0

        if (desired >= Percent(0)) { // braking is based on speed alone; reverse power unnecessary
          if (currentSpeed > -error + Percent(5)) {
            ApplyBrakingPattern(Each(-error / currentSpeed)) // speed always > 0
          } else {
            ApplyBrakingPattern(Percent(100))
          }
        } else { // input < 0, braking with reverse power
          NotBraking(Each(error / (currentSpeed + Percent(100))))
        }
      }
    }
  }

  def dynamicBrakingOutput(targetPower: Stream[Dimensionless],
                           currentSpeed: Stream[Dimensionless]): Stream[Option[Dimensionless]] = {
    val ditherStatus: Stream[DitherStatus] = targetPower.zip(currentSpeed).map { case (target, speed) =>
      calculateBrakeAndDutyCycle(target, speed)
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

      if (requestedPower >= Percent(100)) {
        // we want full brake, so just directly output instead of building up a large total
        (true, 0D, 0D)
      } else if (requestedPower <= Percent(0)) {
        // we want full coast, so just directly output instead of building up a large total
        (false, 0D, 0D)
      } else {
        // combine the accumulated error (requested power vs actual brake percentage
        // with the braking percentage that would result from not braking next
        val windowPattern = accum / (total + 1) + (total / (1 + total))

        // pattern for x% is the opposite of the pattern for (100 - x)%, so we only handle x >= 50% and
        // invert the outputs and requested power otherwise
        val (outputForCoast, requestedPowerAfterInvert) = if (requestedPower.toPercent >= 50) {
          // when we overshoot that means we braked too much so we coast
          (false, requestedPower)
        } else {
          // when we overshoot that means we did not brake often enough (since everything is inverted), so we brake
          (true, Percent(100) - requestedPower) // now it's greater than 50
        }

        if (windowPattern >= requestedPowerAfterInvert.toEach) {
          // reset if overshoot -- the braking percentage even with the next value being coast was too high
          (outputForCoast, 0, (windowPattern - requestedPowerAfterInvert.toEach) * (total + 1))
        } else {
          // if we did not overshoot yet, we continue to brake
          (!outputForCoast, total + 1, accum)
        }
      }
    }.map(_._1)
  }
}
