package com.lynbrookrobotics.potassium.frc

import com.ctre.CANTalon
import com.lynbrookrobotics.potassium.{PeriodicSignal, Signal}
import com.lynbrookrobotics.potassium.units.Ratio
import squants.{Angle, Dimensionless, Each}
import squants.motion.AngularVelocity
import squants.time.Seconds

class TalonEncoder(talon: CANTalon, conversionFactor: Ratio[Angle, Dimensionless]) {

  val drift: Double = talon.getPosition

  val angle: Signal[Angle] = Signal {
    conversionFactor * Each(talon.getPosition - drift)
  }

  val angularVelocity: Signal[AngularVelocity] = Signal {
    (conversionFactor * Each(talon.getSpeed) * 10) / Seconds(1)
  }
}
