package com.lynbrookrobotics.potassium.benchmarks

import com.lynbrookrobotics.potassium.streams.Stream
import squants.space.{Feet, Length}
import squants.time.Nanoseconds
import squants.Time

object ZipPublishBenchmark extends App {
  val (streamB, pubAStream) = Stream.manual[Length]
  val (streamA, pubBStream) = Stream.manual[Length]

  val zipped = streamB.zip(streamA)

  def publishRandom(): Unit = {
    pubAStream(Feet(100) * math.random())
    pubBStream(Feet(100) * math.random())
  }

  def warmUp(ticks: Int): Unit = {
    for (_ <- 1 to ticks) {
      publishRandom()
    }
  }

  def publishAndMeasure(): Time = {
    val startTime = System.nanoTime()
    publishRandom()
    val endTime = System.nanoTime()

    Nanoseconds(endTime - startTime)
  }

  val firstPublish = publishAndMeasure()
  val secondPublish = publishAndMeasure()

  warmUp(1000)
  val durationAfterThousand = publishAndMeasure()

  warmUp(10000)
  val durationAfterTenThousand = publishAndMeasure()

  println(s"first publish: ${firstPublish.toMilliseconds} ms")
  println(s"second publish: ${secondPublish.toMilliseconds} ms")
  println(s"1002th publish: ${durationAfterThousand.toMilliseconds} ms")
  println(s"11002th publish: ${durationAfterTenThousand.toMilliseconds} ms")
}
