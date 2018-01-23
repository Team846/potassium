package com.lynbrookrobotics.potassium.units

import squants.time.{Milliseconds, Time}
import java.util.function.DoubleConsumer

/**
  * Created by the-magical-llamicorn on 4/19/17.
  */
case class Histogram(min: Time, max: Time, bins: Int) {
  final var interval: Time = Milliseconds(.0)
  interval = ((max) - min) / bins
  final protected var histogram: Array[Long] = null
  histogram = new Array[Long](bins + 2)
  // bins and "less than min" and "greater than max"

  def apply(value: Time) {
    if (value < min) {
      histogram(0) += 1; histogram(0) - 1
    }
    else if (value > max) {
      histogram(bins + 1) += 1; histogram(bins + 1) - 1
    }
    else {
      histogram(1 + ((value - min) / interval).toInt) += 1;
      histogram(1 + ((value - min) / interval).toInt) - 1
    }
  }

  override def toString: String = {
    val sb: StringBuilder = new StringBuilder
    sb.append("<")
    sb.append(min)
    sb.append(" : ")
    sb.append(histogram(0))
    sb.append('\n')

    for  { i <- 1 to bins - 1 }
      {
        sb.append((min + (interval * (i - 1))))
        sb.append(" to ")
        sb.append((min + (interval * i)))
        sb.append(" : ")
        sb.append(histogram(i))
        sb.append('\n')
      }

    sb.append(">")
    sb.append(max)
    sb.append(" : ")
    sb.append(histogram(bins + 1))
    sb.append('\n')
    sb.toString
  }
}