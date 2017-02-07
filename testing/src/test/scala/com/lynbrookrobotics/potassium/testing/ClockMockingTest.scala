package com.lynbrookrobotics.potassium.testing

import squants.Time
import squants.time.Milliseconds

import org.scalatest.FunSuite

class ClockMockingTest extends FunSuite {
  test("Single execution of periodic statement") {
    var periodicRun: Boolean = false

    val (mockedClock, trigger) = ClockMocking.mockedClockTicker

    mockedClock(Milliseconds(5)) { _ =>
      periodicRun = true
    }

    trigger(Milliseconds(5))

    assert(periodicRun)
  }

  test("Cancelled periodic statement does not run") {
    var periodicRun: Boolean = false

    val (mockedClock, trigger) = ClockMocking.mockedClockTicker

    val cancel = mockedClock(Milliseconds(5)) { _ =>
      periodicRun = true
    }

    cancel()

    trigger(Milliseconds(5))

    assert(!periodicRun)
  }

  test("Single tick produces correct dt") {
    val (mockedClock, trigger) = ClockMocking.mockedClockTicker

    var dtProduced: Option[Time] = None

    mockedClock(Milliseconds(5)) { dt =>
      dtProduced = Some(dt)
    }

    trigger(Milliseconds(5))

    assert(dtProduced.contains(Milliseconds(5)))
  }

  test("Single execution is correctly executed") {
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
