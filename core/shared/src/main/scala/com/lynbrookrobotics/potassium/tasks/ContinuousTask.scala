package com.lynbrookrobotics.potassium.tasks

import com.lynbrookrobotics.potassium.clock.Clock
import squants.Time

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

  /**
    * Returns a FiniteTask of this ContinuousTask running for the given duration
    * @param duration How long the retunred FiniteTask should run for
    * @return A FiniteTask of this ContinuousTask running for duration amount of time
    */
  def forDuration(duration: Time)(implicit clock: Clock): FiniteTask = {
    new WaitTask(duration).andUntilDone(this)
  }
}

object ContinuousTask {
  def empty: ContinuousTask = new ContinuousTask {
    override protected def onStart(): Unit = {}

    override protected def onEnd(): Unit = {}
  }
}