package com.lynbrookrobotics.potassium.events

import com.lynbrookrobotics.potassium.Signal
import com.lynbrookrobotics.potassium.tasks.{ContinuousTask, Task}

/**
  * An event that has a start, running, and ending phase.
  * @param condition a boolean condition for when the event should be active
  * @param polling the event that is called each time the event should be updated
  */
class ContinuousEvent(condition: => Boolean)(implicit polling: ImpulseEvent) {
  private val onStartSource = new ImpulseEventSource
  private val onEndSource = new ImpulseEventSource

  /**
    * An event that is fired when the continuous event starts
    */
  val onStart: ImpulseEvent = onStartSource.event

  /**
    * An event that is fired when the continuous event ends
    */
  val onEnd: ImpulseEvent = onEndSource.event

  private var tickingCallbacks: List[() => Unit] = List.empty
  private[events] var isRunning = false

  polling.foreach { () =>
    if (condition) {
      tickingCallbacks.foreach(_.apply())

      if (!isRunning) {
        onStartSource.fire()

        isRunning = true
      }
    } else if (isRunning) {
      onEndSource.fire()
      isRunning = false
    }
  }

  /**
    * Adds a listener to be called while the event is running
    * @param onTicking a function to be called continuously when the event is running
    */
  def foreach(onTicking: () => Unit): Unit = {
    tickingCallbacks = onTicking :: tickingCallbacks
  }

  /**
    * Adds a mapping to run a task while the continuous event is running
    * @param task the task to run during the event
    */
  def foreach(task: ContinuousTask): Unit = {
    onStart.foreach { () =>
      Task.abortCurrentTask()
      Task.executeTask(task)
    }

    onEnd.foreach(() => Task.abortTask(task))
  }

  /**
    * Adds a mapping to run a task while the continuous event is running
    * @param task the task to run during the event
    */
  def foreach(task: Signal[ContinuousTask]): Unit = {
    var currentRunningTask: ContinuousTask = null

    onStart.foreach { () =>
      Task.abortCurrentTask()

      currentRunningTask = task.get

      Task.executeTask(currentRunningTask)
    }

    onEnd.foreach(() => Task.abortTask(currentRunningTask))
  }

  /**
    * Returns a continuous event that is an intersection of both events
    * @param event the event to intersect with the original
    */
  def and(event: ContinuousEvent): ContinuousEvent = {
    Signal(event.isRunning && this.isRunning).filter(identity)
  }

  def unary_!(): ContinuousEvent = {
    Signal(!this.isRunning).filter(identity)
  }
}
