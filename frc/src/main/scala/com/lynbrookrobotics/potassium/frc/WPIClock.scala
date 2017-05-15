package com.lynbrookrobotics.potassium.frc

import java.util.concurrent.Semaphore

import com.lynbrookrobotics.potassium.clock.Clock
import edu.wpi.first.wpilibj.{Notifier, Utility}
import squants.Time
import squants.time.{Microseconds, Seconds}

object WPIClock extends Clock {
  override def apply(period: Time)(thunk: (Time) => Unit): Cancel = {
    var lastTime: Option[Time] = None
    var running = true

    val semaphore = new Semaphore(1)
    val thread = new Thread(() => {
      while (running && !Thread.interrupted()) {
        semaphore.acquire()

        val currentTime = Microseconds(Utility.getFPGATime)
        lastTime.foreach { l =>
          thunk(currentTime - l)
        }

        lastTime = Some(currentTime)
      }
    })

    val notifier = new Notifier(() => {
      semaphore.release()
    })

    thread.start()
    notifier.startPeriodic(period.to(Seconds))

    () => {
      notifier.stop()
      running = false
    }
  }

  override def singleExecution(delay: Time)(thunk: => Unit): Unit = {
    val notifier = new Notifier(() => {
      thunk
    })

    notifier.startSingle(delay.to(Seconds))
  }
}
