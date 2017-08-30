package com.lynbrookrobotics.potassium.streams

import org.scalatest.FunSuite
import squants.motion.{FeetPerSecond, Velocity}
import squants.space.{Feet, Length}
import squants.time.{Milliseconds, Nanoseconds}

import scala.collection.immutable.Queue
import com.lynbrookrobotics.potassium.{ClockMocking, Platform}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Promise}

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
    implicit val (clock, update) = ClockMocking.mockedClockTicker

    val (str, pub) = Stream.manualWithTime[Length](Periodic(Milliseconds(1)))
    val derivative = str.derivative

    var lastValue: Velocity = null
    derivative.foreach(lastValue = _)

    pub(Feet(1))

    assert(lastValue == null)

    (1 to 100).foreach { _ =>
      update(Milliseconds(1))
      pub(Feet(1))
      assert(lastValue.toMetersPerSecond == 0)
    }
  }

  test("Derivative of increasing values is always positive") {
    implicit val (clock, update) = ClockMocking.mockedClockTicker
    implicit val tolerance = Feet(0.001) / Milliseconds(1)

    val (str, pub) = Stream.manualWithTime[Length](Periodic(Milliseconds(1)))
    val derivative = str.derivative

    var lastValue: Velocity = null
    derivative.foreach(lastValue = _)

    pub(Feet(0))

    assert(lastValue == null)

    (1 to 100).foreach { n =>
      update(Milliseconds(1))
      pub(Feet(n))
      assert(lastValue ~= Feet(1) / Milliseconds(1))
    }
  }

  test("Integral of zero is always zero") {
    implicit val (clock, update) = ClockMocking.mockedClockTicker

    val (str, pub) = Stream.manualWithTime[Velocity](Periodic(Milliseconds(1)))
    val integral = str.integral

    var lastValue: Length = null
    integral.foreach(lastValue = _)

    pub(FeetPerSecond(0))

    assert(lastValue == null)

    (1 to 100).foreach { _ =>
      update(Milliseconds(1))
      pub(FeetPerSecond(0))
      assert(lastValue.toMeters == 0)
    }
  }

  test("Integral of positive values always increases") {
    implicit val (clock, update) = ClockMocking.mockedClockTicker
    implicit val tolerance = FeetPerSecond(0.001) * Milliseconds(1)

    val (str, pub) = Stream.manualWithTime[Velocity](Periodic(Milliseconds(1)))
    val integral = str.integral

    var lastValue: Length = null
    integral.foreach(lastValue = _)

    pub(FeetPerSecond(0))

    assert(lastValue == null)

    update(Milliseconds(1))
    pub(FeetPerSecond(0))

    (1 to 100).foreach { n =>
      update(Milliseconds(1))
      val prev = lastValue
      pub(FeetPerSecond(1))
      assert(lastValue ~= FeetPerSecond(1) * Milliseconds(n))
    }
  }

  test("Syncing to another stream produces values at correct rate") {
    val (strReference, pubReference) = Stream.manual[Int]
    val (str, pub) = Stream.manual[Int]
    val strSynced = str.syncTo(strReference)

    var lastValue = -1
    strSynced.foreach(lastValue = _)

    assert(lastValue == -1)

    pub(1)

    assert(lastValue == -1)

    pubReference(1)

    assert(lastValue == 1)
  }

  test("Deferring values produces correct values") {
    val (str, pub) = Stream.manual[Int]
    val deferred = str.defer
    var lastValue = -1

    val rootThread = Thread.currentThread()

    val triggeredPromise = Promise[Unit]()
    deferred.foreach { v =>
      if (Platform.isJVM) {
        assert(Thread.currentThread() != rootThread)
      }

      lastValue = v
      triggeredPromise.success(())
    }

    assert(lastValue == -1)

    pub(1)

    if (Platform.isJVM) {
      Await.result(triggeredPromise.future, Duration.Inf)
    }

    assert(lastValue == 1)
  }

  test("Periodically polling a stream produces values at correct rate") {
    implicit val (clock, update) = ClockMocking.mockedClockTicker
    val (str, pub) = Stream.manual[Int]
    val polled = str.pollPeriodic(Milliseconds(5))

    var lastValue = -1
    polled.foreach(lastValue = _)

    assert(lastValue == -1)

    pub(1)

    assert(lastValue == -1)

    update(Milliseconds(5))

    assert(lastValue == 1)
  }

  test("Check callbacks are called with correct values") {
    val (str, pub) = Stream.manual[Int]

    var emitted = -1
    val checked = str.withCheck(emitted = _)

    assert(emitted == -1)

    pub(1)

    assert(emitted == 1)
  }

  test("Filter only lets passed values through") {
    val (str, pub) = Stream.manual[Int]

    var lastValue = -1

    val filtered = str.filter(_ % 2 == 0)
    filtered.foreach(lastValue = _)

    assert(lastValue == -1)

    pub(1)

    assert(lastValue == -1)

    pub(2)

    assert(lastValue == 2)

    pub(4)

    assert(lastValue == 4)

    pub(5)

    assert(lastValue == 4)
  }

  test("Relativized stream produces correct values") {
    val (str, pub) = Stream.manual[Int]

    pub(1)

    var emitted = -1

    val relativized = str.relativize((b, c) => c - b)
    relativized.foreach(emitted = _)

    assert(emitted == -1)

    pub(2)

    assert(emitted == 0)

    pub(3)

    assert(emitted == 1)

    pub(5)

    assert(emitted == 3)
  }

  test("Stream of 3 ft minus Stream of 2 ft produces stream of 1") {
    // minuend is what to subtract from
    val (minuend, pubMinuend) = Stream.manual[Length]

    // subtractand is what to subtract from minuend
    val (subtractand, pubSubtractand) = Stream.manual[Length]

    val difference = minuend.minus(subtractand)

    var lastDifference = Feet(-10)
    difference.foreach(lastDifference = _)

    pubMinuend(Feet(3))
    pubSubtractand(Feet(2))

    assert(lastDifference == Feet(1))
  }
}