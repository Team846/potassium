package com.lynbrookrobotics.potassium

import org.scalacheck.Prop.forAll
import org.scalatest.FunSuite
import org.scalatest.prop.Checkers._
import squants.time.Milliseconds

class PeriodicSignalTest extends FunSuite {
  test("Converted from constant signal") {
    val signal = Signal.constant(1)
    val periodicSignal = signal.toPeriodic

    assert(periodicSignal.currentValue(Milliseconds(5)) == 1)
  }

  test("Converted from variable signal") {
    var value = 1

    val signal = Signal(value)
    val periodicSignal = signal.toPeriodic

    assert(periodicSignal.currentValue(Milliseconds(5)) == 1)

    value = 2

    assert(periodicSignal.currentValue(Milliseconds(5)) == 2)
  }
}
