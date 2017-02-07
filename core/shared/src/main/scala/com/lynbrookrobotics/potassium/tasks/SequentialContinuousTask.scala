package com.lynbrookrobotics.potassium.tasks

/**
  * A continuous task where two subtasks are run sequentially
  * @param first the first task to run
  * @param second the task to run after the first task
  */
class SequentialContinuousTask private[tasks] (first: FiniteTask, second: ContinuousTask)
  extends ContinuousTask with FiniteTaskFinishedListener {
  private var currentPhase: SequentialPhase = Stopped

  override def onFinished(task: FiniteTask): Unit = {
    if (currentPhase == RunningFirst && task == first) {
      currentPhase = RunningSecond
      second.init()
    }
  }

  first.addFinishedListener(this)

  override def onStart(): Unit = {
    currentPhase = RunningFirst
    first.init()
  }

  override def onEnd(): Unit = {
    if (currentPhase == RunningFirst) {
      first.abort()
    } else if (currentPhase == RunningSecond) {
      second.abort()
    }

    currentPhase = Stopped
  }
}
