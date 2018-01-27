package com.lynbrookrobotics.potassium.units

import squants.time.{Milliseconds, Time}
import java.util.function.DoubleConsumer

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
    print(s"< $min : ${bins(0)} \n")

    for  (i <- 1 until binCount) {
        println(s"${bins(i)} to ${min + (interval * (i -  1))} : " +
                s"${min + (interval * i)}")
    }

    println(s"> $max : ${bins(binCount + 1)}")
  }
}