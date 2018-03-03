package com.lynbrookrobotics.potassium.logging

import com.lynbrookrobotics.potassium.clock.{Clock, JavaClock}

object JavaAsyncLogger  extends AsyncLogger {
  override val clock: Clock = JavaClock
}
