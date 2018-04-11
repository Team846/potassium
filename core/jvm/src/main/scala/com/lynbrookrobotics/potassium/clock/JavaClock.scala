package com.lynbrookrobotics.potassium.clock

import java.util.concurrent.{Executors, TimeUnit}

import squants.Time
import squants.time.Milliseconds

/**
  * An implementation of a clock for the JVM that uses a scheduled thread pool
  */
object JavaClock extends Clock {
  private val scheduler = Executors.newScheduledThreadPool(1)

  override def apply(period: Time)(thunk: Time => Unit): Cancel = {
    var lastTime: Option[Time] = None

    val scheduledFuture = scheduler.scheduleAtFixedRate(new Runnable {
      override def run(): Unit = {
        val currentTime = Milliseconds(System.currentTimeMillis())
        lastTime.foreach { l =>
          thunk(currentTime - l)
        }

        lastTime = Some(currentTime)
      }
    }, 0, period.toMilliseconds.toLong, TimeUnit.MILLISECONDS)

    () => {
      scheduledFuture.cancel(true)
    }
  }

  override def singleExecution(delay: Time)(thunk: => Unit): Cancel = {
    val scheduledEvent = scheduler.schedule(new Runnable {
      override def run(): Unit = {
        thunk
      }
    }, delay.to(Milliseconds).toLong, TimeUnit.MILLISECONDS)
    () => {
      scheduledEvent.cancel(false)
    }
  }

  override def currentTime: Time = Milliseconds(System.currentTimeMillis())
}
