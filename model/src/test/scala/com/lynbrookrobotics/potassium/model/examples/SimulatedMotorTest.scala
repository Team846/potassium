package com.lynbrookrobotics.potassium.model.examples

import com.lynbrookrobotics.potassium.ClockMocking
import com.lynbrookrobotics.potassium.model.simulations.SimulatedMotor
import org.scalatest.FunSuite
import squants.Percent
import squants.time.{Milliseconds, Seconds}

class SimulatedMotorTest extends FunSuite {
  val period = Milliseconds(5)

  test("test simulated moter updates when clock is triggered") {
    implicit val (clock, triggerClock) = ClockMocking.mockedClockTicker
    val simulatedMotor = new SimulatedMotor(clock, period)

    var lastOut = Percent(-10)
    simulatedMotor.outputStream.foreach(lastOut = _)

    triggerClock.apply(period)

    assert(lastOut == simulatedMotor.initialOutput)
  }

  test("test simulated motor publishes value from set method") {
    implicit val (clock, triggerClock) = ClockMocking.mockedClockTicker
    val simulatedMotor = new SimulatedMotor(clock, period)

    var lastOut = Percent(-10)
    simulatedMotor.outputStream.foreach(lastOut = _)


    val input = Percent(50)
    simulatedMotor.set(input)
    triggerClock.apply(period)
    assert(lastOut == input)

    val secondInput = Percent(100)
    simulatedMotor.set(secondInput)
    triggerClock.apply(period)
    assert(lastOut == secondInput)
  }
}
