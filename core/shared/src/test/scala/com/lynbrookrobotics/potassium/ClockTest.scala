package com.lynbrookrobotics.potassium

import org.scalatest.FunSuite
import squants.time.Milliseconds

class ClockTest extends FunSuite {
  test("Periodic event produces correct ticks") {
    var fireCount = 0
    val (clock, trigger) = ClockMocking.mockedClockTicker

    clock.periodicEvent(Milliseconds(5)).foreach { () =>
      fireCount += 1
    }

    assert(fireCount == 0)

    trigger(Milliseconds(5))

    assert(fireCount == 1)
  }
}
