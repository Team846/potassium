package com.lynbrookrobotics.potassium.tasks

/**
  * A continuous task where two subtasks are run in parallel
 *
  * @param first one of the tasks to run
  * @param second the other task to run
  */
class ParallelContinuousTask private[tasks](first: ContinuousTask, second: ContinuousTask) extends ContinuousTask {
  override def onStart(): Unit = {
    first.init()
    second.init()
  }

  override def onEnd(): Unit = {
    first.abort()
    second.abort()
  }
}
