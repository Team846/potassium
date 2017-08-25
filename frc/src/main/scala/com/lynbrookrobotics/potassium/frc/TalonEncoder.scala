package com.lynbrookrobotics.potassium.frc

import com.ctre.CANTalon

import com.lynbrookrobotics.potassium.units.Ratio
import squants.{Angle, Dimensionless, Each}
import squants.motion.AngularVelocity
import squants.time.Seconds

class TalonEncoder(talon: CANTalon,
                   conversionFactor: Ratio[Angle, Dimensionless]) {
  def getAngle: Angle = {
    conversionFactor * Each(talon.getPosition)
  }

  def getAngularVelocity: AngularVelocity = {
    (conversionFactor * Each(talon.getSpeed) * 10) / Seconds(1)
  }
}