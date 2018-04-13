package com.lynbrookrobotics.potassium.tasks
import com.lynbrookrobotics.potassium.Component

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

  override val dependencies: Set[Component[_]] = first.dependencies ++ second.dependencies
}
