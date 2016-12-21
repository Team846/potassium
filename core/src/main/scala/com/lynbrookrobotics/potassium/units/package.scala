package com.lynbrookrobotics.potassium

import squants.{Dimensionless, Each, Quantity}

package object units {
  implicit class RichQuantity[N <:  Quantity[N]](val num: N) extends AnyVal {
    def /[D <: Quantity[D]](den: D): Ratio[N, D] = new Ratio(num, den)

    def **[Q <: Quantity[Q]](ratio: Ratio[Q, N]): Q = ratio * num
  }

  implicit def quant2Ratio[N <: Quantity[N]](num: N): Ratio[N, Dimensionless] = new Ratio(num, Each(1))
}
