package com.lynbrookrobotics.potassium.tasks

import com.lynbrookrobotics.potassium.Clock
import squants.Time

class TimeoutFiniteTask(task: FiniteTask, timeout: Time, clock: Clock) extends FiniteTask with FiniteTaskFinishedListener {
  private var currentExecutionID = 0


  override def onFinished(task: FiniteTask): Unit = {
    finished()
  }

  task.addFinishedListener(this)

  override def onStart(): Unit = {
    task.init()
    currentExecutionID += 1

    val latestID = currentExecutionID
    clock.singleExecution(timeout) {
      if (currentExecutionID == latestID) {
        finished()
      }
    }
  }

  override def onEnd(): Unit = {}
}
