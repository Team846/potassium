package com.lynbrookrobotics.potassium

import org.scalatest.FunSuite
import com.lynbrookrobotics.potassium.tasks.FiniteTask
import squants.time.Seconds

class TimeoutFiniteTaskTest extends FunSuite {
  test("Task ends when inner task ends") {
    var task1FinishTrigger: Option[() => Unit] = None

    var task1Started = false
    var task1Ended = false

    val task1 = new FiniteTask {
      override def onStart(): Unit = {
        task1Started = true
      }

      override def onEnd(): Unit = {
        task1Ended = true
      }

      task1FinishTrigger = Some(() => finished())
    }

    implicit val (clock, trigger) = ClockMocking.mockedClockTicker

    val timeoutTask = task1.withTimeout(Seconds(5))

    assert(!task1Started && !task1Ended)

    timeoutTask.init()

    assert(task1Started && !task1Ended)

    task1FinishTrigger.get.apply()

    assert(task1Started && task1Ended && !timeoutTask.isRunning)
  }

  test("Inner task aborted when timeout reached") {
    var task1Started = false
    var task1Ended = false

    val task1 = new FiniteTask {
      override def onStart(): Unit = {
        task1Started = true
      }

      override def onEnd(): Unit = {
        task1Ended = true
      }
    }

    implicit val (clock, trigger) = ClockMocking.mockedClockTicker

    val timeoutTask = task1.withTimeout(Seconds(5))

    assert(!task1Started && !task1Ended)

    timeoutTask.init()

    assert(task1Started && !task1Ended)

    trigger(Seconds(5))

    assert(task1Started && task1Ended && !timeoutTask.isRunning)
  }
}
