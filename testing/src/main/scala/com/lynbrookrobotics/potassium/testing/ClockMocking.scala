package com.lynbrookrobotics.potassium.testing

import com.lynbrookrobotics.potassium.Clock
import squants.Time

object ClockMocking {
  def mockedClockTicker: (Clock, Time => Unit) = {
    var thunks: Map[Time, List[(Time) => Unit]] = Map.empty
    var singleThunks: Map[Time, List[() => Unit]] = Map.empty

    val ticker = new Clock {
      override def apply(period: Time)(thunk: (Time) => Unit): Cancel = {
        thunks = thunks.updated(
          period,
          thunk :: thunks.getOrElse(period, List.empty)
        )

        () => thunks.updated(period, thunks(period).filterNot(_ == thunk))
      }

      override def singleExecution(delay: Time)(thunk: => Unit): Unit = {
        singleThunks = singleThunks.updated(
          delay,
          (() => thunk) :: singleThunks.getOrElse(delay, List.empty)
        )
      }
    }

    (ticker, (period: Time) => {
      thunks.get(period).foreach(l => l.foreach(_(period)))

      singleThunks.get(period).foreach(_.foreach(_.apply()))
      singleThunks = singleThunks.updated(period, List.empty)
    })
  }
}
