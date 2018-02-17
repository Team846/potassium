package com.lynbrookrobotics.potassium.tasks

import com.lynbrookrobotics.potassium.clock.Clock
import squants.Time

/**
  * A finite task that runs a subtask, but kills the subtask when a timeout is triggered
  * @param task the task to run
  * @param timeout the maximum amount of time to allow the task to run
  * @param clock the clock to use to run the timeout
  */
class TimeoutFiniteTask private[tasks](task: FiniteTask, timeout: Time, clock: Clock) extends FiniteTask with FiniteTaskFinishedListener {
  private var currentExecutionID = 0

  override def onFinished(task: FiniteTask): Unit = {
    finished()
  }

  override def onStart(): Unit = {
    task.setFinishedListener(this)
    task.init()
    currentExecutionID += 1

    val latestID = currentExecutionID
    clock.singleExecution(timeout) {
      if (currentExecutionID == latestID) {
        finished()
      }
    }
  }

  override def onEnd(): Unit = {
    if (task.isRunning) {
      task.abort()
    }
  }
}
