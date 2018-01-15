package com.lynbrookrobotics.potassium.units

import squants.time.{Milliseconds, Time}
import java.util.function.DoubleConsumer

/**
  * Created by the-magical-llamicorn on 4/19/17.
  */
case class Histogram(min: Time, max: Time, bins: Int) {
  interval = ((max) - min) / bins
  histogram = new Array[Long](bins + 2)
  // bins and "less than min" and "greater than max"
  final protected var histogram: Array[Long] = null
  final var interval: Time = Milliseconds(.0)

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
    var i: Int = 1
    while (i < bins + 1) {
      {
        sb.append((min + (interval * (i - 1))))
        sb.append(" to ")
        sb.append((min + (interval * i)))
        sb.append(" : ")
        sb.append(histogram(i))
        sb.append('\n')
      }
      {
        i += 1; i - 1
      }
    }
    sb.append(">")
    sb.append(max)
    sb.append(" : ")
    sb.append(histogram(bins + 1))
    sb.append('\n')
    return sb.toString
  }
}