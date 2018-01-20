package com.lynbrookrobotics.potassium.frc

import com.lynbrookrobotics.potassium.clock.Clock
import edu.wpi.first.wpilibj.{Notifier, Utility}
import squants.Time
import squants.time.{Microseconds, Seconds}

private[frc] class WPIClockShared(stopOnException: Boolean) extends Clock {
  override def apply(period: Time)(thunk: (Time) => Unit): Cancel = {
    var lastTime: Option[Time] = None
    var running = false

    val notifier = new Notifier(new Runnable {
      override def run(): Unit = {
        if (!running) {
          running = true
          val currentTime = Microseconds(Utility.getFPGATime)
          lastTime.foreach { l =>
            if (stopOnException) {
              thunk(currentTime - l)
            } else {
              try {
                thunk(currentTime - l)
              } catch {
                case e: Throwable =>
                  e.printStackTrace()
              }
            }
          }

          lastTime = Some(currentTime)
          running = false
        }
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

  override def currentTime: Time = Microseconds(Utility.getFPGATime)
}

object WPIClock extends WPIClockShared(stopOnException = false)

object WPIClockFailFast extends WPIClockShared(stopOnException = true)