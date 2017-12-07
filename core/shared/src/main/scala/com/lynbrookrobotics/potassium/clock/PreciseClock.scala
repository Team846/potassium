package com.lynbrookrobotics.potassium.clock

import squants.Time
object PreciseClock {
  private def loopUntilTrue(condition: () => Boolean) = {
    while (!condition()) {
      // waste time until condition() is true
    }
  }

  def getRealTimeClock(originClock: Clock, noise: Time, tolerance: Time): Clock = {
    new Clock {
      def scheduleAtTime(time: Time, thunk: => Unit) = {
        singleExecution(time - currentTime)(thunk)
      }

      override def apply(period: Time)
                        (thunk: Time => Unit): Cancel = {
        var continueUpdating = true

        var previousUpdateTime: Option[Time] = None


        // schedule oneself for noise amount before the next periodic update
        // then idle in a loop until the actual time is within a tolerance
        lazy val selfSchedulingThunk: (Time, Time) => Unit = (period, target) => {
          var newTarget: Time = target

          // In case that we reach this sub optimal circumstance,
          // recover by setting a new target that is guaranteed to
          // be 1 period from now. This significantly reduces frequency
          // of low period outliers, since we do not try to compensate
          // for a previous high period
          // TODO: doesn't seem to do anything any more?
          val missedTarget = currentTime > target
          if (missedTarget) {
            newTarget = currentTime + period
          }

          def withinTolerance = {
            (currentTime - target).abs <= tolerance
          }

          loopUntilTrue(
            () => withinTolerance || currentTime >= target
          )

          val currTime = currentTime
          previousUpdateTime.foreach { lastTime =>
            thunk(currTime - lastTime)
          }
          previousUpdateTime = Some(currTime)

          if (!missedTarget) {
            newTarget = target + period
          }

          if (continueUpdating) {
            scheduleAtTime(
              target - noise,
              selfSchedulingThunk.apply(period, newTarget))
          }
        }

        selfSchedulingThunk(period, currentTime + period)
        () => continueUpdating = false
      }

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
