package com.lynbrookrobotics.potassium.tasks
import com.lynbrookrobotics.potassium.Component

/**
  * A finite task where two subtasks are run in parallel
  * @param first one of the tasks to run
  * @param second the other task to run
  */
class ParallelFiniteTask private[tasks] (first: FiniteTask, second: FiniteTask)
  extends FiniteTask with FiniteTaskFinishedListener {
  override def onFinished(task: FiniteTask): Unit = {
    if (!first.isRunning && !second.isRunning) {
      finished()
    }
  }

  override def onStart(): Unit = {
    first.setFinishedListener(this)
    second.setFinishedListener(this)

    first.init()
    second.init()
  }

  override def onEnd(): Unit = {
    if (first.isRunning) {
      first.abort()
    }

    if (second.isRunning) {
      second.abort()
    }
  }

  override val dependencies: Set[Component[_]] = first.dependencies ++ second.dependencies
}
