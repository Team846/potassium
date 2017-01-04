package com.lynbrookrobotics.potassium

import org.scalacheck.Prop.forAll
import org.scalatest.FunSuite
import squants.{Dimension, Length, Quantity, UnitOfMeasure}
import squants.electro.Volts
import squants.motion.MetersPerSecond
import squants.space.{Degrees, Feet, Meters}
import squants.time.{Milliseconds, Time, TimeDerivative, TimeIntegral}

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

  test("Derivative of constant signal is always zero") {
    val sig = Signal(Feet(0)).toPeriodic.derivative
    (1 to 100).foreach { _ =>
      assert(sig.currentValue(Milliseconds(5)).toMetersPerSecond == 0)
    }
  }

  test("Derivative of incrementing signal is constant and nonzero") {
    var current = 0
    val sig = Signal {
      current += 1
      Meters(current)
    }.toPeriodic.derivative

    // when we only have recorded one value derivative is zero
    assert(sig.currentValue(Milliseconds(5)).toMetersPerSecond == 0)

    val slope = Meters(1) / Milliseconds(5)

    (1 to 100).foreach { _ =>
      assert(sig.currentValue(Milliseconds(5)) == slope)
    }
  }

  test("Second derivative of quadratic signal is constant and nonzero") {
    var current = 0
    val sig = Signal {
      current += 1
      Meters(current * current)
    }.toPeriodic.derivative.derivative

    // we need 2 values until we start recording the correct 2nd derivative
    assert(sig.currentValue(Milliseconds(5)).toMetersPerSecondSquared == 0)
    sig.currentValue(Milliseconds(5))

    // x^2 -> 2x -> 2
    val slope = Meters(2) / Milliseconds(5) / Milliseconds(5)

    (1 to 100).foreach { _ =>
      val value = sig.currentValue(Milliseconds(5))
      assert(value == slope)
    }
  }

  test("Integral of zero always produces zero") {
    val sig = Signal(MetersPerSecond(0)).toPeriodic.integral
    (1 to 100).foreach { _ =>
      assert(sig.currentValue(Milliseconds(5)).toMeters == 0)
    }
  }

  test("Integral of one always produces ascending values") {
    implicit val tolerance = Meters(0.00000001)

    val sig = Signal(MetersPerSecond(1)).toPeriodic.integral
    (1 to 100).foreach { n =>
      assert(sig.currentValue(Milliseconds(5)) ~= MetersPerSecond(1) * Milliseconds(5) * n)
    }
  }

  test("Derivative of integral is original function") {
    implicit val tolerance = MetersPerSecond(0.00000001)

    val original = Signal(MetersPerSecond(1))
    val shouldBeSame = original.toPeriodic.integral.derivative

    shouldBeSame.currentValue(Milliseconds(5))

    (1 to 100).foreach { _ =>
      assert(shouldBeSame.currentValue(Milliseconds(5)) ~= MetersPerSecond(1))
    }
  }


}
