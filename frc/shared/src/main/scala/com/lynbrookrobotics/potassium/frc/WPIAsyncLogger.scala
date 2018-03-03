package com.lynbrookrobotics.potassium.frc

import com.lynbrookrobotics.potassium.clock.Clock
import com.lynbrookrobotics.potassium.logging.AsyncLogger

object WPIAsyncLogger extends AsyncLogger {
  override val clock: Clock = WPIClock
}
