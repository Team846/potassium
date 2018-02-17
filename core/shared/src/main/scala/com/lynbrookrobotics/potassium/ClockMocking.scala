package com.lynbrookrobotics.potassium

import com.lynbrookrobotics.potassium.clock.Clock
import squants.Time
import squants.time.Seconds

object ClockMocking {
  def mockedClockTicker: (Clock, Time => Unit) = {
    var thunks: List[(Time, () => Unit)] = List.empty
    var _currentTime = Seconds(0)

    val ticker = new Clock {
      // Will break down when clock update is slower than period because
      // thunk updates will have growing lag behind current time
      override def apply(period: Time)(thunk: Time => Unit): Cancel = {
        var lastTimeOfExecution = currentTime

        // lazy is necessary because otherwise compiler doesn't allow this
        // forward reference
        lazy val selfSchedulingThunk: () => Unit = () => {
          // ensure that thunks is scheduled independently of the current time
          // of the clock
          lastTimeOfExecution += period
          thunk(period)

          // self schedule next update
          scheduleAtTime(lastTimeOfExecution + period)(selfSchedulingThunk)
        }

        thunks = (period, selfSchedulingThunk) :: thunks

        () => thunks = thunks.filterNot(_._2 == selfSchedulingThunk)
      }

      private def scheduleAtTime(time: Time)(thunk: () => Unit): Unit = {
        thunks = (time, thunk) :: thunks
      }

      override def singleExecution(delay: Time)(thunk: => Unit): Unit = {
        scheduleAtTime(currentTime + delay)(() => thunk)
      }

      override def currentTime: Time = _currentTime
    }

    (ticker, (period: Time) => {
      _currentTime += period

      thunks.filter{ case (scheduledTime, _) =>
        _currentTime >= scheduledTime
      }.foreach{ case (_, thunk) =>
        thunk()
      }

      thunks = thunks.filterNot{ case (scheduledTime, _) =>
        _currentTime >= scheduledTime
      }
    })
  }
}
