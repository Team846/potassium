package com.lynbrookrobotics.potassium.units

import squants.Quantity

/**
 * Represents a ratio between two quantities
 *
 * @param num the numerator of the ratio
 * @param den the denominator of the ratio
 * @tparam N the units of the numerator
 * @tparam D the units of the denominator
 */
case class Ratio[N <: Quantity[N], D <: Quantity[D]](num: N, den: D) {
  def abs: Ratio[N, D] = Ratio(num.abs, den.abs)
  def value: Double = num.value / den.value

  def >(other: Ratio[N, D]): Boolean = this.value > other.value

  def *(value: Double): Ratio[N, D] = Ratio(value * num, den)
  def *(value: D): N = value / den * num
  def *[D2 <: Quantity[D2]](r: Ratio[D, D2]): Ratio[N, D2] = Ratio(num, r.den) * (r.num / den)

  def recip: Ratio[D, N] = Ratio(den, num)

  override def toString: String = {
    num.toString() + " / " + den.toString()
  }
}
