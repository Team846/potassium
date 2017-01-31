package com.lynbrookrobotics.potassium.frc

import com.lynbrookrobotics.potassium.Signal
import com.lynbrookrobotics.potassium.events.{ContinuousEvent, ImpulseEvent, ImpulseEventSource}
import edu.wpi.first.wpilibj._
import squants.{Dimensionless, Each, Time}
import squants.electro.{ElectricPotential, Volts}
import squants.time.{Frequency, Seconds}

object Implicits {
  // Interface -> signals
  implicit class AnalogInSignals(val analog: AnalogInput) extends AnyVal {
    def voltage: Signal[ElectricPotential] = Signal(Volts(analog.getVoltage))
    def averageVoltage: Signal[ElectricPotential] = Signal(Volts(analog.getAverageVoltage))

    def value: Signal[Int] = Signal(analog.getValue)
    def averageValue: Signal[Int] = Signal(analog.getAverageValue)
  }

  implicit class JoystickSignals(val joystick: Joystick) extends AnyVal {
    def x: Signal[Dimensionless] = Signal(Each(joystick.getX()))
    def y: Signal[Dimensionless] = Signal(Each(joystick.getY()))
    def z: Signal[Dimensionless] = Signal(Each(joystick.getZ()))

    def buttonPressed(button: Int)(implicit polling: ImpulseEvent): ContinuousEvent = {
      Signal(joystick.getRawButton(button)).filter(down => down)
    }
  }

  implicit class CounterSignals(val counter: Counter) extends AnyVal {
    def period: Signal[Time] = Signal(Seconds(counter.getPeriod))
    def frequency: Signal[Frequency] = period.map(t => Each(1) / t)
  }

  // interface conversions
  implicit class DigitalInputConversions(val in: DigitalInput) {
    def toCounter: Counter = new Counter(in)
  }

  implicit class RichDriverStation(val ds: DriverStation) {
    def onDataReceived: ImpulseEvent = {
      val source = new ImpulseEventSource

      new Thread(() => {
        while (!Thread.interrupted()) {
          ds.waitForData()
          source.fire()
        }
      }).start()

      source.event
    }
  }

  implicit def spiToWrapper(spi: SPI): SPIWrapper = new SPIWrapper(spi)

  implicit val clock = WPIClock
}
