package com.lynbrookrobotics.potassium.clock

import java.util.concurrent.{Executors, TimeUnit}

import squants.Time
import squants.time.{Nanoseconds}

/**
  * An implementation of a clock for the JVM that uses a scheduled thread pool
  */
object JavaClock extends Clock {
  private val scheduler = Executors.newScheduledThreadPool(1)

  override def apply(period: Time)(thunk: Time => Unit): Cancel = {
    var lastTime: Option[Time] = None

    val scheduledFuture = scheduler.scheduleAtFixedRate(new Runnable {
      override def run(): Unit = {
        val currentTime = Nanoseconds(System.nanoTime())
        lastTime.foreach { l =>
          thunk(currentTime - l)
        }

        lastTime = Some(currentTime)
      }
    }, 0, period.toNanoseconds.toLong, TimeUnit.NANOSECONDS)

    () => {
      scheduledFuture.cancel(true)
    }
  }

  override def singleExecution(delay: Time)(thunk: => Unit): Unit = {
    scheduler.schedule(new Runnable {
      override def run(): Unit = {
        thunk
      }
    }, delay.toNanoseconds.toLong, TimeUnit.NANOSECONDS)
  }

  override def currentTime: Time = Nanoseconds(System.nanoTime())
}
