package com.lynbrookrobotics.potassium.tasks

/**
  * A finite task where a continuous task is run while a finite task is running
 *
  * @param first the finite task to run
  * @param second the continuous task to run at the same time
  */
class AndUntilDoneTask private[tasks](first: FiniteTask, second: ContinuousTask)
  extends FiniteTask with FiniteTaskFinishedListener {
  override def onFinished(task: FiniteTask): Unit = {
    if (!first.isRunning) {
      finished()
    }
  }

  override def onStart(): Unit = {
    first.setFinishedListener(this)

    first.init()
    second.init()
  }

  override def onEnd(): Unit = {
    if (first.isRunning) {
      first.abort()
    }

    second.abort()
  }
}
