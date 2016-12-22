package com.lynbrookrobotics.potassium

import org.scalameter._
import org.scalatest.FunSuite
import squants.time.Milliseconds

class SignalPerfTest extends FunSuite {
  test("50 values from chain of maps takes less than 5ms", PerfTest) {
    case class ValuesWrapper(v1: Int, v2: Int)
    val signal = Signal(ValuesWrapper(1, 1)).
      map(v => v.copy(v.v1 + 1, v.v2 + 1)).
      map(v => v.copy(v.v1 * 1, v.v2 * 1)).
      map(v => v.copy(v.v1 / 1, v.v2 / 1)).
      map(v => v.copy(v.v1 - 1, v.v2 - 1)).
      map(v => v.copy(v.v1 % 1, v.v2 % 1))

    val time = measure {
      (1 to 50).map(_ => signal.get)
    }

    assert(time.value <= 5 && time.units == "ms")
  }

  test("50 values from chain of periodic maps takes less than 5ms", PerfTest) {
    case class ValuesWrapper(v1: Int, v2: Int)
    val signal = Signal(ValuesWrapper(1, 1)).toPeriodic.
      map((v, dt) => v.copy(v.v1 + 1, v.v2 + 1)).
      map((v, dt) => v.copy(v.v1 * 1, v.v2 * 1)).
      map((v, dt) => v.copy(v.v1 / 1, v.v2 / 1)).
      map((v, dt) => v.copy(v.v1 - 1, v.v2 - 1)).
      map((v, dt) => v.copy(v.v1 % 1, v.v2 % 1))

    val time = measure {
      (1 to 50).map(_ => signal.currentValue(Milliseconds(5)))
    }

    assert(time.value <= 5 && time.units == "ms")
  }
}
