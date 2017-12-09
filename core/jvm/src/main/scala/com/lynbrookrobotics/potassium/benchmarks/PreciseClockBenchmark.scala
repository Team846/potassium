package com.lynbrookrobotics.potassium.benchmarks

import java.io.PrintWriter
import java.lang.management.{GarbageCollectorMXBean, ManagementFactory}

import com.lynbrookrobotics.potassium.clock.{JavaClock, PreciseClock}
import squants.time.{Milliseconds, Minutes, Seconds, Time}

object PreciseClockBenchmark {
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
          tolerance = Milliseconds(0.01),
          name
        )
      } else {
        JavaClock
      }

      val logFileName = if (usePreciseClock) {
        s"precise time noise: ${noise.toMilliseconds}"
      } else {
        "regular time"
      }

      val logDump = new PrintWriter(logFileName)

      val startTime = clock.currentTime
      clock.apply(period){ dt =>
        println(s"$name: ${dt.toMilliseconds} ${(clock.currentTime - startTime).toSeconds}")
        logDump.println(dt.toSeconds)
        logDump.flush()
        Thread.sleep(100)
      }
    }

    startAndLogClock(Seconds(5), Seconds(0.5), usePreciseClock = true, name = "clock 1")
    startAndLogClock(Seconds(5), Seconds(0.5), usePreciseClock = true, name = "clock 2")
    //startAndLogClock(Milliseconds(5), Milliseconds(3), usePreciseClock = true)

    //startAndLogClock(Milliseconds(5), null, usePreciseClock = false)
  }
}
