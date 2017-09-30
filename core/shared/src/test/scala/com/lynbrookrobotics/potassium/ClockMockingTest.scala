package com.lynbrookrobotics.potassium

import squants.Time
import squants.time.{Milliseconds, Seconds}
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

  test("Single execution still functions when clock update don't exactly coincide with scheduled time") {
    val (mockedClock, trigger) = ClockMocking.mockedClockTicker

    var executed = false
    mockedClock.singleExecution(Seconds(10)) {
      executed = true
    }

    trigger(Seconds(5))
    assert(!executed)

    trigger(Seconds(6))

    // At time 11 seconds, thunk for 10 seconds was executed
    assert(executed)
  }
}
