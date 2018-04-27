package com.lynbrookrobotics.potassium.logging

import squants.time.Time

class Histogram(min: Time, max: Time, binCount: Int, asyncLogger: AsyncLogger) {
  val interval = (max - min) / binCount

  // bins and "less than min" and "greater than max"
  val bins = new Array[Long](binCount + 2)

  def apply(value: Time): Unit = {
    if (value < min) {
      bins(0) += 1
    } else if (value > max) {
      bins(binCount + 1) += 1
    } else {
      bins(1 + ((value - min) / interval).toInt) += 1
    }
  }

  def printStatus(): Unit = {
    asyncLogger.info(s"< $min : ${bins(0)} \n")

    for (i <- 1 until binCount) {
      asyncLogger.info(
        s"${bins(i)} to ${min + (interval * (i - 1))} : ${min + (interval * i)}"
      )
    }

    asyncLogger.info(s"> $max : ${bins(binCount + 1)}")
  }
}
