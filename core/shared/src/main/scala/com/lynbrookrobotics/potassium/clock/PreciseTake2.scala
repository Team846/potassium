package com.lynbrookrobotics.potassium.clock
import squants.Time

class PreciseTake2 extends Clock {
  override def apply(period: Time)(thunk: Time => Unit) = ???

  override def singleExecution(delay: Time)(thunk: => Unit) = ???

  override def currentTime = ???
}
