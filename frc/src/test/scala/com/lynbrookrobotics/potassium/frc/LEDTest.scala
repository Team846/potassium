package com.lynbrookrobotics.potassium.frc

import com.lynbrookrobotics.potassium.ClockMocking
import org.scalatest.FunSuite

class LEDTest extends FunSuite{
  test("Test lighting"){
    implicit val (clock, trigger) = ClockMocking.mockedClockTicker
  }
}
