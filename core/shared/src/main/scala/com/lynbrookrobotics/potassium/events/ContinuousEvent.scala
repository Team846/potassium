package com.lynbrookrobotics.potassium.events

import com.lynbrookrobotics.potassium.clock.Clock
import com.lynbrookrobotics.potassium.tasks.{ContinuousTask, Task}
import squants.Time

/**
  * Contains configuration for continuous event condition polling
  * @param clock the clock to use to periodically check the condition
  * @param period the period to check the condition at
  */
case class EventPolling(clock: Clock, period: Time)

/**
  * An event that has a start, running, and ending phase.
  * @param condition a boolean condition for when the event should be active
  * @param polling the configuration for periodically checking if the event should be active
  */
class ContinuousEvent(condition: => Boolean)(implicit polling: EventPolling) {
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
  private var isRunning = false

  polling.clock(polling.period) { _ =>
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
    onStart.foreach(() => Task.executeTask(task))
    onEnd.foreach(() => Task.abortTask(task))
  }
}
