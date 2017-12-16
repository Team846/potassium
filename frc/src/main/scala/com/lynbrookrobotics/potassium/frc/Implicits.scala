package com.lynbrookrobotics.potassium.frc

import com.lynbrookrobotics.potassium.streams
import com.lynbrookrobotics.potassium.streams._
import com.lynbrookrobotics.potassium.clock.Clock
import com.lynbrookrobotics.potassium.events.{ContinuousEvent, ImpulseEvent, ImpulseEventSource, PollingContinuousEvent}
import edu.wpi.first.wpilibj._
import squants.{Dimensionless, Each, Time}
import squants.electro.{ElectricPotential, Volts}
import squants.time.{Frequency, Seconds}

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
      * @param period how frequently to check if the button is pressed
      * @param clock used to update checking of the button status
      * @return a ContinuousEvent true whenenver a button is pressed at the
      *         buttonId of this joystick
      */
    def buttonPressedEvent(buttonId: Int,
                           period: Time)
                          (implicit clock: Clock): ContinuousEvent = {
      Stream.periodic(period){
        joystick.getRawButton(buttonId)
      }.evenWithCondition(down => down)
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
