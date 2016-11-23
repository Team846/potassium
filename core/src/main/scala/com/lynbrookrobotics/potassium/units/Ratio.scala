package com.lynbrookrobotics.potassium.units

import squants.Quantity

class Ratio[N <: Quantity[N], D <: Quantity[D]](num: N, den: D) {
  def *(value: D): N = value / den * num
}
