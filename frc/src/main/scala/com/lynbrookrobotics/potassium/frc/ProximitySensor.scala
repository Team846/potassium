package com.lynbrookrobotics.potassium.frc

import edu.wpi.first.wpilibj.AnalogInput

class ProximitySensor(channel: Int) {
  var sensor:AnalogInput = new AnalogInput(channel)

  def getVoltage: Double = {
    sensor.getAverageVoltage
  }

  def isCloserThan(distance: Double): Boolean = {
    getVoltage >= distance
  }
}
