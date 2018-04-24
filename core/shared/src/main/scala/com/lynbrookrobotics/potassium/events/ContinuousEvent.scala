package com.lynbrookrobotics.potassium.events

import com.lynbrookrobotics.potassium.Signal
import com.lynbrookrobotics.potassium.tasks.{ContinuousTask, Task}

/**
  * An event that has a start, running, and ending phase.
  */
class ContinuousEvent {
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

  private var onEventTrueCallbacks: List[() => Unit] = List.empty
  private var onUpdateCallbacks: List[Boolean => Unit] = List.empty

  private[events] var isRunning = false

  protected def updateEventState(eventTrue: Boolean): Unit = {
    onUpdateCallbacks.foreach(_.apply(eventTrue))

    if (eventTrue) {
      onEventTrueCallbacks.foreach(_.apply())

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
    * Adds a listener to be called while the event is happening (aka, condition returns true)
    * @param onTrue a function to be called continuously when the event is happening
    */
  def foreach(onTrue: () => Unit): Unit = {
    onEventTrueCallbacks = onTrue :: onEventTrueCallbacks
  }


  /**
    * For each update of the event, regardless if the event is true or not
    * @param onUpdate
    */
  private[events] def foreachUpdate(onUpdate: Boolean => Unit) = {
    onUpdateCallbacks = onUpdate :: onUpdateCallbacks
  }

  /**
    * Adds a mapping to run a task while the continuous event is running
    * @param task the task to run during the event
    */
  def foreach(task: ContinuousTask): Unit = {
    onStart.foreach { () =>
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
      currentRunningTask = task.get

      Task.executeTask(currentRunningTask)
    }

    onEnd.foreach(() => Task.abortTask(currentRunningTask))
  }

  /**
    * Returns a continuous event that is an intersection of both events
    * @param other the event to intersect with the original
    */
  def &&(other: ContinuousEvent): ContinuousEvent = {
    val (intersectionEvent, updateAndEvent) = ContinuousEvent.newEvent

    var parentATrue = false
    var parentBTrue = false

    this.foreachUpdate { isTrue =>
      parentATrue = isTrue
      updateAndEvent.apply(parentATrue && parentBTrue)
    }

    other.foreachUpdate { isTrue =>
      parentBTrue = isTrue
      updateAndEvent.apply(parentATrue && parentBTrue)
    }

    intersectionEvent
  }

  def unary_!(): ContinuousEvent = {
    val (negatedEvent, updateNegatedEventState) = ContinuousEvent.newEvent
    this.foreachUpdate(parentEventTrue => updateNegatedEventState(!parentEventTrue))
    negatedEvent
  }
}

object ContinuousEvent {
  /**
    * @return a ContinuousEvent and a function to update whether the event
    *         is true or not
    */
  def newEvent: (ContinuousEvent, Boolean => Unit) = {
    val ret = new ContinuousEvent
    (ret, condition => ret.updateEventState(condition))
  }
}
