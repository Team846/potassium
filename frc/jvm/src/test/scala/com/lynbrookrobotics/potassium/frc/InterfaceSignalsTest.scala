package com.lynbrookrobotics.potassium.frc

import edu.wpi.first.wpilibj.AnalogInput
import org.scalacheck.Prop.forAll
import org.scalatest.FunSuite
import org.scalatest.prop.Checkers._
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import Implicits._
import squants.electro.Volts

class InterfaceSignalsTest extends FunSuite with MockitoSugar {
  test("Analog input produces correct voltage") {
    val mockedAnalogIn = mock[AnalogInput]

    check(forAll { d: Double =>
      when(mockedAnalogIn.getVoltage).thenReturn(d)

      mockedAnalogIn.voltage.toVolts == d
    })
  }

  test("Analog input produces correct average voltage") {
    val mockedAnalogIn = mock[AnalogInput]

    check(forAll { d: Double =>
      when(mockedAnalogIn.getAverageVoltage).thenReturn(d)

      mockedAnalogIn.averageVoltage.toVolts == d
    })
  }
}
