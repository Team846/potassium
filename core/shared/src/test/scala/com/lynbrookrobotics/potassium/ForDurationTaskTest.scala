package com.lynbrookrobotics.potassium

import com.lynbrookrobotics.potassium.tasks.{ContinuousTask, WaitTask}
import org.scalatest.FunSuite
import squants.time.Seconds

class ForDurationTaskTest extends FunSuite {
  test("task produced from forDuration lasts correct duration") {
    implicit val (clock, trigger) = ClockMocking.mockedClockTicker
    val durationTask = ContinuousTask.empty.forDuration(Seconds(5))
    durationTask.init()

    val runningAtStart = durationTask.isRunning

    trigger(Seconds(1))
    val runningBeforeEnd = durationTask.isRunning

    trigger(Seconds(4))
    val runningAtEnd = durationTask.isRunning

    trigger(Seconds(1))
    val runningAfterEnd = durationTask.isRunning

    assert(runningAtStart && runningBeforeEnd && !runningAtEnd && !runningAfterEnd)
  }
}
