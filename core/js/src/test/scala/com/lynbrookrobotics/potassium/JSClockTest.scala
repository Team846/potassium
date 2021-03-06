package com.lynbrookrobotics.potassium

import com.lynbrookrobotics.potassium.clock.JSClock
import org.scalatest.AsyncFunSuite
import squants.time.Milliseconds

import scala.concurrent.Promise

class JSClockTest extends AsyncFunSuite {
  implicit override def executionContext =
    scala.concurrent.ExecutionContext.Implicits.global

  test("Periodic update produces at correct interval") {
    val start = System.currentTimeMillis()
    var count = 0
    val testDonePromise = Promise[Long]()
    val future = testDonePromise.future

    var cancel: Option[JSClock.Cancel] = None
    cancel = Some(JSClock(Milliseconds(5)) { _ =>
      count += 1
      if (count == 100) {
        cancel.foreach(_.apply())

        val timeTaken = System.currentTimeMillis() - start
        testDonePromise.success(math.abs(timeTaken - 500))
      }
    })

    future.map(t => assert(t <= 500))
  }

  test("Single execution happens at correct time") {
    val start = System.currentTimeMillis()
    val testDonePromise = Promise[Long]()
    val future = testDonePromise.future

    JSClock.singleExecution(Milliseconds(500)) {
      val timeTaken = System.currentTimeMillis() - start
      testDonePromise.success(math.abs(timeTaken - 500))
    }

    future.map(t => assert(t <= 250))
  }

  test("Single execution can be canceled") {
    val promise = Promise[Boolean]

    JSClock.singleExecution(Milliseconds(0)) {
      promise.success(false)
    }()

    JSClock.singleExecution(Milliseconds(250)) {
      promise.success(true)
    }

    promise.future.map { v =>
      assert(v)
    }
  }
}
