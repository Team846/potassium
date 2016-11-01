package com.lynbrookrobotics.potassium

import java.util.concurrent.{Executors, TimeUnit}

import squants.Time
import squants.time.Milliseconds

trait Clock {
  type Cancel = () => Unit

  def apply(period: Time)(thunk: Time => Unit): Cancel
  def singleExecution(delay: Time)(thunk: => Unit): Unit
}

object JavaClock extends Clock {
  val scheduler = Executors.newScheduledThreadPool(1)

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
    }, 0, period.to(Milliseconds).toLong, TimeUnit.MILLISECONDS)

    () => {
      scheduledFuture.cancel(true)
    }
  }

  override def singleExecution(delay: Time)(thunk: => Unit): Unit = {
    scheduler.schedule(new Runnable {
      override def run(): Unit = {
        thunk
      }
    }, delay.to(Milliseconds).toLong, TimeUnit.MILLISECONDS)
  }
}