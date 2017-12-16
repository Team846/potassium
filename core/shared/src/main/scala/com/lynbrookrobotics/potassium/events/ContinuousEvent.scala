package com.lynbrookrobotics.potassium.events

import com.lynbrookrobotics.potassium.Signal
import com.lynbrookrobotics.potassium.tasks.{ContinuousTask, Task}

/**
  * An event that has a start, running, and ending phase.
  * @param condition a boolean condition for when the event should be active
  * @param polling the event that is called each time the event should be updated
  */
@deprecated
class PollingContinuousEvent(condition: => Boolean)(implicit polling: ImpulseEvent) {
  private val onStartSource = new ImpulseEventSource
  private val onEndSource = new ImpulseEventSource

  private[PollingContinuousEvent] def conditionValue: Boolean = condition

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
  def &&(event: PollingContinuousEvent): PollingContinuousEvent = {
    Signal(event.conditionValue && this.conditionValue).filter(identity)
  }

  def unary_!(): PollingContinuousEvent = {
    Signal(!conditionValue).filter(identity)
  }
}


class ContinuousEvent(val condition: () => Boolean) {
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
  // used to check negation events
  private var onEventFalseCallbacks: List[() => Unit] = List.empty

  private[events] var isRunning = false

  def checkCondition(): Unit = {
    if (condition.apply()) {
      onEventTrueCallbacks.foreach(_.apply())

      if (!isRunning) {
        onStartSource.fire()

        isRunning = true
      }
    } else if (isRunning) {
      onEventFalseCallbacks.foreach(_.apply())
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
    * To be used exclusively for unary_!
    * Same as foreach, except runs this function whenever the even is NOT happening
    * (aka, condition returns false)
    * @param onFalse a function to be called continuously when the event is NOT happening
    */
  def foreachNotTrue(onFalse: () => Unit): Unit = {
    onEventFalseCallbacks = onFalse :: onEventFalseCallbacks
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
  def &&(event: ContinuousEvent): ContinuousEvent = {
    val isIntersection = () => this.condition.apply() && event.condition.apply()
    val (intersectionEvent, checkIntersectionEvent) = ContinuousEvent.newEvent(isIntersection)

    this.foreach(checkIntersectionEvent)
    event.foreach(checkIntersectionEvent)

    intersectionEvent
  }

  def unary_!(): ContinuousEvent = {
    val isNegated = () => {
      !condition.apply()
    }
    val (negatedEvent, checkEventNegated) = ContinuousEvent.newEvent(isNegated)
    this.foreachNotTrue(checkEventNegated)

    negatedEvent
  }
}

object ContinuousEvent {
  def newEvent(condition: () => Boolean): (ContinuousEvent, () => Unit) = {
    val ret = new ContinuousEvent(condition)
    (ret, () => ret.checkCondition())
  }
}