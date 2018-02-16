package com.lynbrookrobotics.potassium.benchmarks

import com.lynbrookrobotics.potassium.ClockMocking
import com.lynbrookrobotics.potassium.streams.{Periodic, Stream}
import squants.time.Milliseconds
import org.scalameter._

object StreamBenchmarking {
  def benchmarkEvaluation[I, O](generator: => I)(pipeline: Stream[I] => Stream[O]): Quantity[Double] = {
    implicit val (clock, tick) = ClockMocking.mockedClockTicker

    val inputStream = Stream.periodic[I](Milliseconds(5))(generator)
    val outputStream = pipeline(inputStream)
    val outputCancel = outputStream.foreach(_ => {})

    val ret = config(
      Key.exec.benchRuns -> 100000
    ) withWarmer {
      new Warmer.Default
    } withMeasurer {
      new Measurer.IgnoringGC
    } measure {
      tick(Milliseconds(5))
    }

    outputCancel()

    ret
  }

  def runBenchmarkEvaluation[I, O](name: String)(generator: => I)(pipeline: Stream[I] => Stream[O]): Unit = {
    println(s"Average time for $name - ${benchmarkEvaluation(generator)(pipeline)}")
  }
}
