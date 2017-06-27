package com.lynbrookrobotics.potassium.streams

import com.lynbrookrobotics.potassium.ClockMocking
import org.scalatest.FunSuite
import squants.motion.{FeetPerSecond, Velocity}
import squants.space.{Feet, Length}
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

  test("Derivative of constant values is always zero") {
    val (str, pub) = Stream.manual[Length]
    val derivative = str.derivative

    var lastValue: Velocity = null
    derivative.foreach(lastValue = _)

    pub(Feet(1))

    assert(lastValue == null)

    (1 to 100).foreach { _ =>
      Thread.sleep(1)
      pub(Feet(1))
      assert(lastValue.toMetersPerSecond == 0)
    }
  }

  test("Derivative of increasing values is always positive") {
    val (str, pub) = Stream.manual[Length]
    val derivative = str.derivative

    var lastValue: Velocity = null
    derivative.foreach(lastValue = _)

    pub(Feet(0))

    assert(lastValue == null)

    (1 to 100).foreach { n =>
      Thread.sleep(1)
      pub(Feet(n))
      assert(lastValue.toMetersPerSecond > 0)
    }
  }

  test("Integral of zero is always zero") {
    val (str, pub) = Stream.manual[Velocity]
    val integral = str.integral

    var lastValue: Length = null
    integral.foreach(lastValue = _)

    pub(FeetPerSecond(0))

    assert(lastValue == null)

    (1 to 100).foreach { _ =>
      Thread.sleep(1)
      pub(FeetPerSecond(0))
      assert(lastValue.toMeters == 0)
    }
  }

  test("Integral of positive values always increases") {
    val (str, pub) = Stream.manual[Velocity]
    val integral = str.integral

    var lastValue: Length = null
    integral.foreach(lastValue = _)

    pub(FeetPerSecond(1))

    assert(lastValue == null)

    pub(FeetPerSecond(1))

    (1 to 100).foreach { _ =>
      Thread.sleep(1)
      val prev = lastValue
      pub(FeetPerSecond(1))
      assert(lastValue > prev)
    }
  }
}
