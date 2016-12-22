package com.lynbrookrobotics.potassium.clock

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
  def singleExecution(delay: Time)(thunk: => Unit): Unit
}
