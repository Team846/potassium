package com.lynbrookrobotics.potassium

import com.lynbrookrobotics.potassium.tasks.WaitTask
import org.scalatest.FunSuite
import squants.Time
import squants.time.Seconds

class WaitTaskTest extends FunSuite {
  test("Wait tasks waits correct duration") {
    implicit val (clock, trigger) = ClockMocking.mockedClockTicker
    val waitTask = new WaitTask(Seconds(5))
    waitTask.init()

    val beforeTrigger = waitTask.isRunning
    trigger(Seconds(5))
    val afterTrigger = waitTask.isRunning

    assert(beforeTrigger == true &&
            afterTrigger == false)
  }
}
