package com.lynbrookrobotics.potassium.clock

import com.lynbrookrobotics.potassium.events.{ImpulseEvent, ImpulseEventSource}
import squants.Time

/**
 * An interface that can schedule timed events.
 */
trait Clock {
  type Cancel = () => Unit

  /**
   * Schedules a periodic execution
   * @param period the period between executions
   * @param thunk the block of code to execute
   * @return a function to cancel the execution
   */
  def apply(period: Time)(thunk: Time => Unit): Cancel

  /**
   * Schedules a single execution of a function
   * @param delay the initial delay before running the function
   * @param thunk the function to execute after the delay
   */
  def singleExecution(delay: Time)(thunk: => Unit): Cancel

  /**
   * Creates an impulse event that fires at a fixed rate
   * @param period the period to fire the event at
   * @return an event that fires at the given rate
   */
  def periodicEvent(period: Time): ImpulseEvent = {
    val source = new ImpulseEventSource

    apply(period) { _ =>
      source.fire()
    }

    source.event
  }

  def currentTime: Time
}
