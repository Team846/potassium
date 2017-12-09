package com.lynbrookrobotics.potassium.clock

import squants.Time

import scala.collection.mutable.ArrayBuffer

object PreciseClock {
  private var loopers = Map.empty[() => Boolean, () => Unit]

  def add(condition: () => Boolean, after: () => Unit): Unit = {
    loopers = loopers.updated(condition, after)
    startLoop()
  }

  private def startLoop(): Unit = {
    while (loopers.nonEmpty) {
      loopers.foreach { case (condition, after) =>
        if (condition()) {
          loopers = loopers.filterNot{ case (key, _) => key == condition }
          after()
        }
      }
    }
  }
}

class PreciseClock(originClock: Clock, noise: Time, tolerance: Time, name: String) extends Clock {
  private def scheduleAtTime(time: Time, thunk: => Unit) = {
    singleExecution(time - currentTime)(thunk)
  }

  private def loopUntilTrue(condition: () => Boolean) = {
    println(s"start $name busy wait")
    while (!condition()) {
      // waste time until condition() is true
    }
    println(s"end $name busy wait")
  }

  override def apply(period: Time)(thunk: Time => Unit): Cancel = {
    var continueUpdating = true

    var previousUpdateTime: Option[Time] = None

    var running = false

    // schedule oneself for noise amount before the next periodic update
    // then idle in a loop until the actual time is within a tolerance
    lazy val selfSchedulingThunk: Time => Unit = (target) => {
      val tempCurrTime = currentTime

      val targetGarunteedInFuture = if (tempCurrTime > target) {

        // oh shoot! We've already past the ideally future target time.
        // Recover by using a new target guaranteed to be in the
        // future by 1 period from now. This significantly reduces frequency
        // of low period outliers, since we do not try to compensate
        // for previous very long period outliers.
        println(s"past the target at $name")
        println(s"current time: ${tempCurrTime.toSeconds} newTarget: ${(tempCurrTime + period).toSeconds}")
        println()
        tempCurrTime + period
      } else {
        target
      }

      loopUntilTrue { () =>
        val withinTolerance = (currentTime - targetGarunteedInFuture).abs <= tolerance
        val pastTheTarget = currentTime >= targetGarunteedInFuture
        withinTolerance || pastTheTarget
      }

      if (continueUpdating) {
        val newTarget = targetGarunteedInFuture + period
        scheduleAtTime(
          newTarget - noise,
          selfSchedulingThunk.apply(newTarget)
        )
      }

      if (!running) {
        running = true
        val currTime = currentTime
        previousUpdateTime.foreach { lastTime =>
          thunk(currTime - lastTime)
        }
        previousUpdateTime = Some(currTime)
        running = false
      } else {
        println("nooooooooooooooo")
      }
    }

    selfSchedulingThunk(currentTime + period)
    () => continueUpdating = false

  }

  override def singleExecution(delay: Time)(thunk: => Unit): Unit = {
    originClock.singleExecution(delay)(thunk)
  }

  override def currentTime: Time = originClock.currentTime
}

