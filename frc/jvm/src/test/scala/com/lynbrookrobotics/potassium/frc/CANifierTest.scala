package com.lynbrookrobotics.potassium.frc

import com.ctre.phoenix.CANifier
import com.lynbrookrobotics.potassium.{ClockMocking, Signal}
import edu.wpi.first.wpilibj.DriverStation
import org.scalatest.mockito.MockitoSugar
import org.scalatest.FunSuite
import com.lynbrookrobotics.potassium.streams._
import edu.wpi.first.wpilibj.DriverStation.Alliance
import org.mockito.Mockito._
import org.mockito.{ArgumentCaptor, ArgumentMatchers}
import squants.time.Milliseconds

class CANifierTest extends FunSuite with MockitoSugar{

  test("Test custom color class"){
    val color = Color.rgb(255, 255, 255)
    assert(color.red == 255 && color.blue == 255 && color.green == 255)
  }

  test("Test color not within 0 to 255"){
    val lowerColor = Color.rgb(-10, -10, -10)
    assert(lowerColor == Color.rgb(0, 0, 0))
    val higherColor = Color.rgb(300, 300, 300)
    assert(higherColor == Color.rgb(255, 255, 255))
  }

  test("Test HSV conversion"){
    val color0 = Color.hsv(0, 1, 1)
    val color60 = Color.hsv(60, 1, 1)
    val color120 = Color.hsv(120, 1, 1)
    val color180 = Color.hsv(180, 1, 1)
    val color240 = Color.hsv(240, 1, 1)
    val color300 = Color.hsv(300, 1, 1)
    val color359 = Color.hsv(359, 1, 1)
    assert(color0 == Color.rgb(255, 0, 0))
    assert(color60 == Color.rgb(255, 255, 0))
    assert(color120 == Color.rgb(0, 255, 0))
    assert(color180 == Color.rgb(0, 255, 255))
    assert(color240 == Color.rgb(0, 0, 255))
    assert(color300 == Color.rgb(255, 0, 255))
    assert(color359 == Color.rgb(255, 0, 4))
  }

  test("Test component"){
    val period = Milliseconds(10)
    implicit val (mockedClock, trigger) = ClockMocking.mockedClockTicker
    val mockedCanifier = mock[CANifier]
    val argumentCaptor: ArgumentCaptor[Double] = ArgumentCaptor.forClass(0.0.getClass)
    val hardware = LEDControllerHardware(mockedCanifier)
    val coreTicks = Stream.periodic[Unit](period)()
    val alliance = Signal.constant(Alliance.Red)
    val component = new LEDController(coreTicks, alliance)(hardware)
    component.resetToDefault()
    trigger.apply(period)
    component.setController(Stream.periodic[Color](period)(Color.rgb(0, 255, 255)))
    trigger.apply(period)
    verify(mockedCanifier, times(2)).setLEDOutput(argumentCaptor.capture(), ArgumentMatchers.eq(CANifier.LEDChannel.LEDChannelA))
    verify(mockedCanifier, times(2)).setLEDOutput(argumentCaptor.capture(), ArgumentMatchers.eq(CANifier.LEDChannel.LEDChannelB))
    verify(mockedCanifier, times(2)).setLEDOutput(argumentCaptor.capture(), ArgumentMatchers.eq(CANifier.LEDChannel.LEDChannelC))
    val arguments = argumentCaptor.getAllValues
    assert(arguments.get(0) == 0.0)
    assert(arguments.get(1) == 1.0)
    assert(arguments.get(2) == 1.0)
    assert(arguments.get(3) == 0.0)
    assert(arguments.get(4) == 0.0)
    assert(arguments.get(5) == 1.0)

  }
}
