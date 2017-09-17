package com.lynbrookrobotics.potassium.tasks

import com.lynbrookrobotics.potassium.clock.Clock
import squants.Time

class WaitTask(time: Time)(implicit clock: Clock) extends FiniteTask {
  private var currentExecutionID = 0

  override def onStart(): Unit = {
    currentExecutionID += 1

    val latestID = currentExecutionID
    clock.singleExecution(time) {
      if (currentExecutionID == latestID) {
        finished()
      }
    }
  }

  override def onEnd(): Unit = {}
}
