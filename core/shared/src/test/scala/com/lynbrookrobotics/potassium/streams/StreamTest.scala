package com.lynbrookrobotics.potassium.streams

import com.lynbrookrobotics.potassium.ClockMocking
import org.scalatest.FunSuite
import squants.time.Milliseconds

import scala.collection.immutable.Queue

class StreamTest extends FunSuite {
  test("Manually created stream runs callbacks appropriately") {
    val (str, pub) = Stream.manual[Int]

    var lastPublishedValue = -1
    str.foreach(v => lastPublishedValue = v)

    assert(lastPublishedValue == -1)

    pub(1)

    assert(lastPublishedValue == 1)

    pub(2)

    assert(lastPublishedValue == 2)
  }

  test("Period stream is triggered correctly") {
    implicit val (clock, trigger) = ClockMocking.mockedClockTicker

    var count = 0
    val stream = Stream.periodic(Milliseconds(5)) {}
    stream.foreach(_ => count += 1)

    assert(count == 0)

    trigger(Milliseconds(5))

    assert(count == 1)

    trigger(Milliseconds(5))

    assert(count == 2)
  }

  test("Mapping stream produces correct values") {
    val (str, pub) = Stream.manual[Int]
    var lastValue = -1

    val mapped = str.map(_ + 1)
    mapped.foreach(lastValue = _)

    assert(lastValue == -1)

    pub(0)

    assert(lastValue == 1)
  }

  test("Sliding over a stream produces correct values") {
    val (str, pub) = Stream.manual[Int]
    var lastValue: Queue[Int] = null
    val slid = str.sliding(2)
    slid.foreach(lastValue = _)

    assert(lastValue == null)

    pub(1)

    assert(lastValue == null)

    pub(2)

    assert(lastValue == Queue(1, 2))

    pub(3)

    assert(lastValue == Queue(2, 3))
  }
}
