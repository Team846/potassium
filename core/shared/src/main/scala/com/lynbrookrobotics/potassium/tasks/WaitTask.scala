package com.lynbrookrobotics.potassium.tasks

import com.lynbrookrobotics.potassium.clock.Clock
import squants.Time

class WaitTask(time: Time)(implicit clock: Clock) extends FiniteTask {
  private var currentExecutionID = 0

  override def onStart(): Unit = {
    currentExecutionID += 1

    val latestID = currentExecutionID
    clock.singleExecution(time) {
      // TODO: should be variable timePassed, and check should time against time?
      // TODO: single execution compares against dt and not total changed time?
      if (currentExecutionID == latestID) {
        finished()
      }
    }
  }

  override def onEnd(): Unit = {}
}
