package com.lynbrookrobotics.potassium.tasks

import com.lynbrookrobotics.potassium.clock.Clock
import squants.Time

/**
  * A interface for tasks that need to listen to finish events from other tasks. This is used
  * for task composers such as [[SequentialTask]] and [[TimeoutFiniteTask]]
  */
trait FiniteTaskFinishedListener {
  def onFinished(task: FiniteTask): Unit
}

/**
  * A task that can stop itself by calling `finished()` when it is done
  */
abstract class FiniteTask extends Task { self =>
  private var running = false
  private var listener: Option[FiniteTaskFinishedListener] = None

  /**
    * Adds a task finish listener to listen to this task.
    * @param listener the listener of finish events
    */
  def setFinishedListener(newListener: FiniteTaskFinishedListener): Unit = {
    assert(listener == None)
    listener = Some(newListener)
  }

  /**
    * Marks the task as finished. This should be called inside an implementation
    * of FiniteTask when the task is complete.
    */
  protected final def finished(): Unit = {
    if (running) {
      onEnd()
      running = false

      listener.foreach(_.onFinished(this))
      listener = None
    }
  }

  protected def onStart(): Unit
  protected def onEnd(): Unit

  def isRunning: Boolean = running

  override def init(): Unit = {
    if (!running) {
      running = true
      onStart()
    }
  }

  override def abort(): Unit = {
    if (running) {
      onEnd()
      running = false

      listener = None
    }
  }

  /**
    * Creates a sequential task with the given task to be run after this task has finished
    * @param that the task to run second
    * @return a sequential task combining both tasks
    */
  def then(that: FiniteTask): FiniteTask = {
    new SequentialTask(this, that)
  }

  /**
    * Creates a continuous task with the given task to be run after this task has finished
    * @param that the task to run second
    * @return a sequential task combining both tasks
    */
  def then(that: ContinuousTask): ContinuousTask = {
    new SequentialContinuousTask(this, that)
  }

  /**
    * Creates a task that runs this task and the given task in parallel
    * @param that the other task to run
    * @return a parallel task combining both tasks
    */
  def and(that: FiniteTask): FiniteTask = {
    new ParallelFiniteTask(this, that)
  }

  /**
    * Creates a task that runs a continuous task while this task is running
    * @param that the continuous task to run
    * @return a finite task that runs this and the continuous task
    */
  def andUntilDone(that: ContinuousTask): FiniteTask = {
    new AndUntilDoneTask(this, that)
  }

  /**
    * Creates a finite task that runs this task but kills it after the timeout
    * @param timeout the maximum duration to run the task
    * @param clock the clock to time the timeout
    * @return the task with a timeout
    */
  def withTimeout(timeout: Time)(implicit clock: Clock): FiniteTask = {
    new TimeoutFiniteTask(this, timeout, clock)
  }

  def toContinuous: ContinuousTask = new ContinuousTask {
    override def onStart() = self.init()

    override def onEnd() = self.abort()
  }
}

object FiniteTask {
  final def empty: FiniteTask = new FiniteTask {
    override def onStart(): Unit = finished()

    override def onEnd(): Unit = {}
  }
}
