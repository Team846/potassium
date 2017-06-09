package com.lynbrookrobotics.potassium.streams

import com.lynbrookrobotics.potassium.ClockMocking
import org.scalatest.FunSuite
import squants.time.Milliseconds

class StreamTest extends FunSuite {
  test("Manually created signal runs callbacks appropriately") {
    val (sig, pub) = Stream.manual[Int]

    var lastPublishedValue = -1
    sig.foreach(v => lastPublishedValue = v)

    assert(lastPublishedValue == -1)

    pub(1)

    assert(lastPublishedValue == 1)

    pub(2)

    assert(lastPublishedValue == 2)
  }

  test("Period signal is triggered correctly") {
    implicit val (clock, trigger) = ClockMocking.mockedClockTicker

    var count = 0
    val signal = Stream.periodic(Milliseconds(5)) {}
    signal.foreach(_ => count += 1)

    assert(count == 0)

    trigger(Milliseconds(5))

    assert(count == 1)

    trigger(Milliseconds(5))

    assert(count == 2)
  }
}
