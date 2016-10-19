package com.lynbrookrobotics.potassium

import org.scalacheck.Prop.forAll
import org.scalatest.FunSuite
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

  test("Checks get executed on every calculation") {
    var checkResult = 0

    val signal = Signal.constant(1)
    val periodicSignal = signal.toPeriodic.withCheck { v =>
      checkResult = v
    }

    assert(periodicSignal.currentValue(Milliseconds(5)) == 1)
    assert(checkResult == 1)
  }

  test("Each periodic signal requested only once per request") {
    var numRequests = 0
    val signal = Signal {
      numRequests += 1
      0
    }.toPeriodic

    val periodicCombined = signal.zip(signal)

    assert(periodicCombined.currentValue(Milliseconds(5)) == (0, 0))
    assert(numRequests == 1)
  }
}
