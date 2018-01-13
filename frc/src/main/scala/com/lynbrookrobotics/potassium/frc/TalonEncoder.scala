package com.lynbrookrobotics.potassium.frc

import com.ctre.CANTalon
//import com.ctre.phoenix.motorcontrol.FeedbackDevice
import com.lynbrookrobotics.potassium.units.Ratio
import squants.motion.AngularVelocity
import squants.time.Seconds
import squants.{Angle, Dimensionless, Each}

class TalonEncoder(talon: CANTalon,
                   conversionFactor: Ratio[Angle, Dimensionless]) {

//  talon.configSelectedFeedbackSensor(FeedbackDevice.QuadEncoder, 0, 10)

  def getAngle: Angle = {
    conversionFactor * Each(talon.getPosition)
  }

  def getAngularVelocity: AngularVelocity = {
    // we multiply by 10 because we read in units of ticks/100ms
//    (conversionFactor * Each(talon.getSelectedSensorVelocity(0)) * 10) / Seconds(1)
    (conversionFactor * Each(talon.getSpeed) * 10) / Seconds(1)
  }
}