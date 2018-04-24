package com.lynbrookrobotics.potassium.benchmarks

import StreamBenchmarking._
import com.lynbrookrobotics.potassium.Signal
import com.lynbrookrobotics.potassium.control.{PIDConfig, PIDF, PIDFConfig}
import com.lynbrookrobotics.potassium.units.{GenericIntegral, GenericValue, Ratio}
import com.lynbrookrobotics.potassium.units.GenericValue._
import squants.{Dimensionless, Percent}
import squants.motion.{MetersPerSecond, Velocity}
import squants.space.{Length, Meters}
import squants.time.Seconds

object CoreBenchmarks extends App {
  runBenchmarkEvaluation("Stream.map(double + random)")(math.random)(
    _.map(_ + math.random)
  )

  (0 to 4).foreach { n =>
    val size = (2 * math.pow(5, n)).toInt
    runBenchmarkEvaluation(s"Stream.sliding($size)")(math.random)(
      _.sliding(size)
    )
  }

  runBenchmarkEvaluation("Stream.zipWithTime")(math.random)(
    _.zipWithTime
  )

  runBenchmarkEvaluation("Stream.derivative")(Meters(math.random))(
    _.derivative
  )

  runBenchmarkEvaluation("Stream.scanLeft(_ + _)")(math.random)(
    _.scanLeft(0D)(_ + _)
  )

  runBenchmarkEvaluation("Stream.integral")(MetersPerSecond(math.random))(
    _.integral
  )

  runBenchmarkEvaluation("Stream branch followed by zip")(math.random)(
    stream => stream.map(_ * math.random).zip(stream.map(_ * math.random))
  )

  runBenchmarkEvaluation("PID end-to-end")(Meters(math.random))(
    current => {
      val gains = Signal.constant(
        PIDConfig[Length, Length, GenericValue[Length], Velocity, GenericIntegral[Length], Dimensionless](
          kp = Ratio(Percent(100), Meters(1)),
          ki = Ratio(Percent(100), Meters(1).toGeneric * Seconds(1)),
          kd = Ratio(Percent(100), Meters(1) / Seconds(1))
        )
      )

      PIDF.pid(
        current,
        current.mapToConstant(Meters(5)),
        gains
      )
    }
  )
}
