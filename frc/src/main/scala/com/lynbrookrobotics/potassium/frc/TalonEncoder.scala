package com.lynbrookrobotics.potassium.frc

import com.ctre.CANTalon

import com.lynbrookrobotics.potassium.streams.Stream
import com.lynbrookrobotics.potassium.units.Ratio
import Implicits.clock
import squants.{Angle, Dimensionless, Each}
import squants.motion.AngularVelocity
import squants.time.Seconds
import squants.Time

// TODO: Requires detailed review
class TalonEncoder(talon: CANTalon,
                   talonUpdateRate: Time,
                   conversionFactor: Ratio[Angle, Dimensionless]) {
  // TODO: Not granteed to be synchronous with other data
  val angle: Stream[Angle] = Stream.periodic(talonUpdateRate) (
    conversionFactor * Each(talon.getPosition)
  )

  val angularVelocity: Stream[AngularVelocity] = Stream.periodic(talonUpdateRate) (
    (conversionFactor * Each(talon.getSpeed) * 10) / Seconds(1)
  )
}
