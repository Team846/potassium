package com.lynbrookrobotics.potassium.testing

import com.lynbrookrobotics.potassium.Clock
import squants.Time

object ClockMocking {
  def mockedClockTicker = {
    var thunks: Map[Time, List[(Time) => Unit]] = Map.empty

    val ticker = new Clock {
      override def apply(period: Time)(thunk: (Time) => Unit): Cancel = {
        thunks = thunks.updated(
          period,
          thunk :: thunks.getOrElse(period, List.empty)
        )

        () => thunks.updated(period, thunks(period).filterNot(_ == thunk))
      }
    }

    (ticker, (period: Time) => {
      thunks.get(period).foreach(l => l.foreach(_(period)))
    })
  }
}
