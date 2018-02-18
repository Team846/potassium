package com.lynbrookrobotics.potassium.units

import squants.time.Time


import com.team846.frc2015.logging.AsyncLogger

/**
  * Created by the-magical-llamicorn on 4/19/17.
  */
class Histogram(min: Time, max: Time, binCount: Int) {
  val interval = (max - min) / binCount

  // bins and "less than min" and "greater than max"
  val bins = new Array[Long](binCount + 2)

  def apply(value: Time) {
    if (value < min) {
      bins(0) += 1
    } else if (value > max) {
      bins(binCount + 1) += 1
    } else {
      bins(1 + ((value - min) / interval).toInt) += 1
    }
  }

  def printStatus(): Unit =  {
    AsyncLogger.info(s"< $min : ${bins(0)} \n")

    for  (i <- 1 until binCount) {
        AsyncLogger.info(s"${bins(i)} to ${min + (interval * (i -  1))} : " +
                s"${min + (interval * i)}")
    }

    AsyncLogger.info(s"> $max : ${bins(binCount + 1)}")
  }
}