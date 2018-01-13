package com.lynbrookrobotics.potassium.frc
//import com.ctre.phoenix.motorcontrol.FeedbackDevice
import com.lynbrookrobotics.potassium.units.Ratio
import squants.motion.AngularVelocity
import squants.time.Seconds
import squants.{Angle, Dimensionless, Each}

class TalonEncoder(talon: TalonController,
                   conversionFactor: Ratio[Angle, Dimensionless]) {

  def getAngle: Angle = {
    conversionFactor * Each(talon.rawPosition)
  }

  def getAngularVelocity: AngularVelocity = {
    // we multiply by 10 because we read in units of ticks/100ms
    (conversionFactor * Each(talon.rawPosition) * 10) / Seconds(1)
  }
}