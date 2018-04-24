package com.lynbrookrobotics.potassium.frc

import com.lynbrookrobotics.potassium.events.{ContinuousEvent, ImpulseEvent, ImpulseEventSource}
import com.lynbrookrobotics.potassium.streams._
import edu.wpi.first.wpilibj._
import squants.electro.{ElectricPotential, Volts}
import squants.time.{Frequency, Seconds}
import squants.{Dimensionless, Each, Time}

object Implicits {
  // Interface -> signals
  implicit class AnalogInSignals(val analog: AnalogInput) extends AnyVal {
    def voltage: ElectricPotential = Volts(analog.getVoltage)
    def averageVoltage: ElectricPotential = Volts(analog.getAverageVoltage)

    def value: Int = analog.getValue
    def averageValue: Int = analog.getAverageValue
  }

  implicit class JoystickSignals(val joystick: Joystick) extends AnyVal {
    def x: Dimensionless = Each(joystick.getX())
    def y: Dimensionless = Each(joystick.getY())
    def z: Dimensionless = Each(joystick.getZ())

    /**
     *
     * @param buttonId the id of the button to check
     * @param updateSource update the status of the button event with each update
     *                     of the updateSource stream
     * @return a ContinuousEvent true whenever a button is pressed at the
     *         buttonId of this joystick
     */
    def buttonPressedEvent(buttonId: Int, updateSource: Stream[Unit]): ContinuousEvent = {
      updateSource.map(_ => joystick.getRawButton(buttonId)).eventWhen(down => down)
    }
  }

  implicit class CounterSignals(val counter: Counter) extends AnyVal {
    def period: Time = Seconds(counter.getPeriod)
    def frequency: Frequency = Each(1) / period
  }

  // interface conversions
  implicit class DigitalInputConversions(val in: DigitalInput) {
    def toCounter: Counter = new Counter(in)
  }

  implicit class RichDriverStation(val ds: DriverStation) {
    def onDataReceived: ImpulseEvent = {
      val source = new ImpulseEventSource

      new Thread(new Runnable {
        override def run(): Unit = {
          while (!Thread.interrupted()) {
            ds.waitForData()
            source.fire()
          }
        }
      }).start()

      source.event
    }
  }

  implicit def spiToWrapper(spi: SPI): SPIWrapper = new SPIWrapper(spi)

  implicit val clock = WPIClock
}
