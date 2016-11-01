package com.lynbrookrobotics.potassium.frc

import com.lynbrookrobotics.potassium.Clock
import edu.wpi.first.wpilibj.{Notifier, Utility}
import squants.Time
import squants.time.{Microseconds, Seconds}

object WPIClock extends Clock {
  override def apply(period: Time)(thunk: (Time) => Unit): Cancel = {
    var lastTime: Option[Time] = None
    val currentTime = Microseconds(Utility.getFPGATime)

    val notifier = new Notifier(new Runnable {
      override def run(): Unit = {
        lastTime.foreach { l =>
          thunk(currentTime - l)
        }

        lastTime = Some(currentTime)
      }
    })

    notifier.startPeriodic(period.to(Seconds))

    () => notifier.stop()
  }

  override def singleExecution(delay: Time)(thunk: => Unit): Unit = {
    val notifier = new Notifier(new Runnable {
      override def run(): Unit = {
        thunk
      }
    })

    notifier.startSingle(delay.to(Seconds))
  }
}
