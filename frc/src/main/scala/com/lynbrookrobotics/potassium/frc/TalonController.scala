package com.lynbrookrobotics.potassium.frc

import com.ctre.phoenix.motorcontrol.{FeedbackDevice, VelocityMeasPeriod}
import com.ctre.phoenix.motorcontrol.can.TalonSRX

class TalonController(deviceNumber: Int) {
  val talon = new TalonSRX(deviceNumber)

  talon.configSelectedFeedbackSensor(FeedbackDevice.QuadEncoder, 0, 10)
  talon.configVelocityMeasurementPeriod(VelocityMeasPeriod.Period_5Ms, 0)
  talon.configVelocityMeasurementWindow(64, 0)

  def follow (talonController: TalonController): Unit = talon.follow(talonController.talon)

  def rawPosition: Int = talon.getSelectedSensorPosition(0)
  def rawVelocity: Int = talon.getSelectedSensorVelocity(0)
}