package com.lynbrookrobotics.potassium.frc

import com.lynbrookrobotics.potassium.Signal
import com.lynbrookrobotics.potassium.events.{ContinuousEvent, EventPolling}
import edu.wpi.first.wpilibj.{AnalogInput, Counter, Joystick}
import squants.{Dimensionless, Each, Time}
import squants.electro.Volts
import squants.time.{Frequency, Seconds}

object Implicits {
  // Interface -> signals
  implicit class AnalogInSignals(val analog: AnalogInput) extends AnyVal {
    def voltage = Signal(Volts(analog.getVoltage))
    def averageVoltage = Signal(Volts(analog.getAverageVoltage))

    def value = Signal(analog.getValue)
    def averageValue = Signal(analog.getAverageValue)
  }

  implicit class JoystickSignals(val joystick: Joystick) extends AnyVal {
    def x: Signal[Dimensionless] = Signal(Each(joystick.getX()))
    def y: Signal[Dimensionless] = Signal(Each(joystick.getY()))
    def z: Signal[Dimensionless] = Signal(Each(joystick.getZ()))

    def buttonPressed(button: Int)(implicit polling: EventPolling): ContinuousEvent = {
      Signal(joystick.getRawButton(button)).filter(down => down)
    }
  }

  implicit class CounterSignals(val counter: Counter) extends AnyVal {
    def period: Signal[Time] = Signal(Seconds(counter.getPeriod))
    def frequency: Signal[Frequency] = period.map(t => Each(1) / t)
  }

  implicit val clock = WPIClock
}
