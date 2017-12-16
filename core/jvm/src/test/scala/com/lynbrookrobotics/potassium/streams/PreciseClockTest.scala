package com.lynbrookrobotics.potassium.streams

import java.io.PrintWriter

import com.lynbrookrobotics.potassium.clock.{JavaClock, PreciseClock}
import squants.time._

object PreciseClockTest {
  def main(args: Array[String]): Unit = {
    def startAndLogClock(period: Time,
                         noise: Time,
                         usePreciseClock: Boolean,
                         log: Boolean = true,
                         name: String) = {
      val clock = if (usePreciseClock) {
        new PreciseClock(
          originClock = JavaClock,
          noise,
          tolerance = Milliseconds(0.0),
          name
        )
      } else {
        JavaClock
      }

      val logFileName = if (usePreciseClock) {
        s"$name noise: ${noise.toMilliseconds}"
      } else {
        "regular time"
      }

      val logDump = new PrintWriter(logFileName)

      val startTime = clock.currentTime
      clock.apply(period){ dt =>
        println(s"$name: ${dt.toMilliseconds}")
        logDump.println(dt.toMilliseconds)
        logDump.flush()
      }
    }


    startAndLogClock(
      period = Milliseconds(5),
      noise = Milliseconds(2),
      usePreciseClock = true,
      name = "precise clock 1")

    startAndLogClock(
      period = Milliseconds(5),
      noise = Milliseconds(2),
      usePreciseClock = true,
      name = "precise clock 2")

    startAndLogClock(
      period = Milliseconds(5),
      noise = Milliseconds(2),
      usePreciseClock = true,
      name = "precise clock 3")


    startAndLogClock(
      period = Milliseconds(5),
      noise = null,
      usePreciseClock = false,
      name = "regular")
  }
}
