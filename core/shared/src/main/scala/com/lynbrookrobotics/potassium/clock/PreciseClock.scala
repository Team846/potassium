package com.lynbrookrobotics.potassium.clock

import squants.Time
object PreciseClock {
  private def loopUntilTrue(condition: () => Boolean) = {
    while (!condition()) {
      // waste time until condition() is true
    }
  }

  def getPreciseClock(originClock: Clock, noise: Time, tolerance: Time): Clock = {
    new Clock {
      def scheduleAtTime(time: Time, thunk: => Unit) = {
        singleExecution(time - currentTime)(thunk)
      }
asdf asdf as pls no compile
//      override def apply(period: Time)
//                        (thunk: Time => Unit): Cancel = {
//        var continueUpdating = true
//
//        var previousUpdateTime: Option[Time] = None
//        // schedule oneself for noise amount before the next periodic update
//        // then idle in a loop until the actual time is within a tolerance
//        lazy val selfSchedulingThunk: (Time, Time) => Unit = (period, target) => {
//          println("some update")
//          val targetGarunteedInFuture = if (currentTime > target) {
//            // oh shoot, we've already past the target time.
//            // recover by setting a new target that is guaranteed to
//            // be 1 period from now. This significantly reduces frequency
//            // of low period outliers, since we do not try to compensate
//            // for a previous very long period
//            println("missed target")
//            currentTime + period
//          } else {
//            println("didn't miss")
//            target
//          }
//
//          def withinTolerance = {
//            (currentTime - targetGarunteedInFuture).abs <= tolerance
//          }
//
//          loopUntilTrue(
//            () => withinTolerance || currentTime >= targetGarunteedInFuture
//          )
//
//          val currTime = currentTime
//          previousUpdateTime.foreach { lastTime =>
//            thunk(currTime - lastTime)
//          }
//          previousUpdateTime = Some(currTime)
//
//          if (continueUpdating) {
//            val newTarget = targetGarunteedInFuture + period
//            scheduleAtTime(
//              newTarget,
//              selfSchedulingThunk.apply(period, newTarget))
//          }
//        }
//
//        selfSchedulingThunk(period, currentTime + period)
//        () => continueUpdating = false
//      }

      override def currentTime: Time = {
        originClock.currentTime
      }

      override def singleExecution(delay: Time)
                                  (thunk: => Unit): Unit = {
        originClock.singleExecution(delay)(thunk)
      }
    }
  }
}

