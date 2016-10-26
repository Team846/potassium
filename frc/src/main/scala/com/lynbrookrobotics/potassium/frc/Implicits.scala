package com.lynbrookrobotics.potassium.frc

import com.lynbrookrobotics.potassium.Signal
import edu.wpi.first.wpilibj.{AnalogInput, Joystick}
import squants.Each
import squants.electro.Volts

object Implicits {
  // Interface -> signals
  implicit class AnalogInSignals(val analog: AnalogInput) extends AnyVal {
    def voltage = Signal(Volts(analog.getVoltage))
    def averageVoltage = Signal(Volts(analog.getAverageVoltage))

    def value = Signal(analog.getValue)
    def averageValue = Signal(analog.getAverageValue)
  }

  implicit class JoystickSignals(val joystick: Joystick) extends AnyVal {
    def x = Signal(Each(joystick.getX()))
    def y = Signal(Each(joystick.getY()))
    def z = Signal(Each(joystick.getZ()))
  }
}
