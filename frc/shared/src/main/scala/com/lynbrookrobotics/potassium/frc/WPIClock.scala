package com.lynbrookrobotics.potassium.frc

import com.lynbrookrobotics.potassium.clock.Clock
import edu.wpi.first.wpilibj.{Notifier, RobotController, Utility}
import squants.Time
import squants.time.{Microseconds, Seconds}

private[frc] class WPIClockShared(stopOnException: Boolean) extends Clock {
  override def apply(period: Time)(thunk: Time => Unit): Cancel = {
    var lastTime: Time = Microseconds(RobotController.getFPGATime)

    val notifier = new Notifier(new Runnable {
      override def run(): Unit = {
        val currentTime = Microseconds(RobotController.getFPGATime)
        if (stopOnException) {
          thunk(currentTime - lastTime)
        } else {
          try {
            thunk(currentTime - lastTime)
          } catch {
            case e: Throwable =>
              e.printStackTrace()
          }
        }

        lastTime = currentTime
      }
    })

    notifier.startPeriodic(period.to(Seconds))

    () =>
      notifier.stop()
  }

  override def singleExecution(delay: Time)(thunk: => Unit): Cancel = {
    var notifier: Notifier = null
    notifier = new Notifier(new Runnable {
      override def run(): Unit = {
        thunk
        notifier.stop()
      }
    })

    notifier.startSingle(delay.to(Seconds))
    () =>
      notifier.stop()
  }

  override def currentTime: Time = Microseconds(RobotController.getFPGATime)
}

object WPIClock extends WPIClockShared(stopOnException = false)

object WPIClockFailFast extends WPIClockShared(stopOnException = true)
