package com.lynbrookrobotics.potassium.testing

import squants.Time
import squants.time.Milliseconds

import org.scalatest.FunSuite

class ClockMockingTest extends FunSuite {
  test("Single execution of periodic statement") {
    var periodicRun: Boolean = false

    val (mockedClock, trigger) = ClockMocking.mockedClockTicker

    mockedClock(Milliseconds(5)) { dt =>
      periodicRun = true
    }

    trigger(Milliseconds(5))

    assert(periodicRun)
  }

  test("Single tick produces correct dt") {
    val (mockedClock, trigger) = ClockMocking.mockedClockTicker

    var dtProduced: Time = null

    mockedClock(Milliseconds(5)) { dt =>
      dtProduced = dt
    }

    trigger(Milliseconds(5))

    assert(dtProduced == Milliseconds(5))
  }

  test("Single execution is correclty executed") {
    val (mockedClock, trigger) = ClockMocking.mockedClockTicker

    var executed = false

    mockedClock.singleExecution(Milliseconds(5)) {
      executed = true
    }

    trigger(Milliseconds(1))

    assert(!executed)

    trigger(Milliseconds(5))

    assert(executed)
  }
}
