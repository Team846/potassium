package com.lynbrookrobotics.potassium.commons.electronics

import squants.QuantityRange
import squants.electro.{ElectricPotential, Volts}
import squants.motion.AngularVelocity

object DynamicBraking {
  private val ditherPatterns = List(
    "............", // 0
    "|...........", // 1
    "|.....|.....", // 2
    "|...|...|...", // 3
    "|..|..|..|..", // 4
    "|.|.|.|..|..", // 5
    "|.|.|.|.|.|.", // 6
    "||.||.|.|.|.", // 7
    "||.||.||.||.", // 8
    "|||.|||.|||.", // 9
    "|||||.|||||.", // 10
    "|||||||||||.", // 11
    "||||||||||||" //  12
  )
    .map(_.toCharArray)
    .map(_.map(_ == '|'))
  private val ditherLength = ditherPatterns.map(_.length).min

  def orBrake(max: ElectricPotential, free: AngularVelocity)
             (tick: Int, target: ElectricPotential, current: AngularVelocity)
  : Option[ElectricPotential] = {
    val sp = target / max
    val pv = current / free
    if (QuantityRange(0, pv.abs) contains sp)
      if (ditherPatterns
      (((pv / sp) * ditherPatterns.length).toInt)
      (tick.abs % ditherLength)
      ) Some(Volts(0))
      else None
    else if (QuantityRange(
      math.min(sp, pv),
      math.max(sp, pv)
    ) contains 0) Some((sp - pv) * max)
    else Some(target)
  }
}
