package com.lynbrookrobotics.potassium.units

import squants.Quantity

/**
  * Represents a ratio between two quantities
  * @param num the numerator of the ratio
  * @param den the denominator of the ratio
  * @tparam N the units of the numerator
  * @tparam D the units of the denominator
  */
class Ratio[N <: Quantity[N], D <: Quantity[D]](num: N, den: D) {
  def *(value: D): N = value / den * num

  override def toString: String = {
    num.toString() + " / " + den.toString()
  }
}
