package com.lynbrookrobotics.potassium.tasks
import com.lynbrookrobotics.potassium.Component

sealed trait SequentialPhase
case object Stopped extends SequentialPhase
case object RunningFirst extends SequentialPhase
case object RunningSecond extends SequentialPhase

/**
  * A finite task where two subtasks are run sequentially
  * @param first the first task to run
  * @param second the task to run after the first task
  */
class SequentialTask private[tasks] (first: FiniteTask, second: FiniteTask)
  extends FiniteTask with FiniteTaskFinishedListener {
  private var currentPhase: SequentialPhase = Stopped

  override def onFinished(task: FiniteTask): Unit = {
    if (currentPhase == RunningFirst && task == first) {
      currentPhase = RunningSecond
      second.setFinishedListener(this)
      second.init()
    } else if (currentPhase == RunningSecond && task == second) {
      finished()
    }
  }



  override def onStart(): Unit = {
    currentPhase = RunningFirst
    first.setFinishedListener(this)
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

  override val dependencies: Set[Component[_]] = first.dependencies ++ second.dependencies
}
