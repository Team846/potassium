package com.lynbrookrobotics.potassium.clock

import org.scalajs.dom

import squants.Time
import squants.time.Milliseconds

/**
  * An implementation of a clock for JS, using window.setInterval and window.setTimeout
  */
object JSClock extends Clock {
  override def apply(period: Time)(thunk: (Time) => Unit): Cancel = {
    var lastTime: Option[Time] = None

    val intervalId = dom.window.setInterval(() => {
      val currentTime = Milliseconds(System.currentTimeMillis())
      lastTime.foreach { l =>
        thunk(currentTime - l)
      }

      lastTime = Some(currentTime)
    }, period.toMilliseconds)

    () => {
      dom.window.clearInterval(intervalId)
    }
  }

  override def singleExecution(delay: Time)(thunk: => Unit): Cancel = {
    val taskToken = dom.window.setTimeout(() => {
      thunk
    }, delay.toMilliseconds)
    () => {
      dom.window.clearTimeout(taskToken)
    }
  }

  override def currentTime: Time = Milliseconds(System.currentTimeMillis())
}
