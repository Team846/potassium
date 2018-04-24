package com.lynbrookrobotics.potassium.frc

import edu.wpi.first.wpilibj.AnalogInput
import squants.Length

/**
 * Implements interface for sharp IR sensor
 * datasheet:https://acroname.com/articles/linearizing-sharp-ranger-data
 * @param channel
 */
class ProximitySensor(channel: Int) {
  val sensor: AnalogInput = new AnalogInput(channel)

  def getVoltage: Double = {
    sensor.getAverageVoltage
  }

  def isCloserThan(distance: Length): Boolean = {
    var minVoltage: Double = 1 / (distance.toCentimeters + 0.42)
    getVoltage > minVoltage
  }
}
