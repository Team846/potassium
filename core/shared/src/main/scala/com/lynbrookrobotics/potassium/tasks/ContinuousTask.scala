package com.lynbrookrobotics.potassium.tasks

/**
  * Represents a task that can only be stopped by an external impulse.
  */
abstract class ContinuousTask extends Task {
  protected def onStart(): Unit
  protected def onEnd(): Unit

  /**
    * Creates a continuous task where two tasks are run in parallel
    * @param that the other task to run
    * @return a task that runs both tasks
    */
  def and(that: ContinuousTask): ContinuousTask = {
    new ParallelContinuousTask(this, that)
  }

  /**
    * Starts the continuous task.
    */
  override def init(): Unit = onStart()

  /**
    * Stops the continuous task.
    */
  override def abort(): Unit = onEnd()
}
