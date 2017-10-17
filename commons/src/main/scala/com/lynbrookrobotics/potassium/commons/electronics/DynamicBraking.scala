package com.lynbrookrobotics.potassium.commons.electronics

import squants.electro.{ElectricPotential, Volts}
import squants.motion.AngularVelocity

object DynamicBraking {
  // | -> short
  // . -> 0 volts
  private val brakingPatterns = List(
    "||||||||||||", // strongest braking
    ".|||||||||||",
    ".|||||.|||||",
    ".|||.|||.|||",
    ".||.||.||.||",
    ".|.|.|.||.||",
    ".|.|.|.|.|.|",
    "..|..|.|.|.|",
    "..|..|..|..|",
    "...|...|...|",
    ".....|.....|",
    "...........|"  // weakest braking
  )
    .map(_.toCharArray)
    .map(_.map(_ == '|'))
  private val ditherLength = brakingPatterns.map(_.length).min

  def orBrake(max: ElectricPotential, free: AngularVelocity)
             (tick: Int, target: ElectricPotential, current: AngularVelocity)
  : Option[ElectricPotential] = {
    val sp = target / max
    val pv = current / free
    if ((pv < 0) != (sp < 0)) Some((sp - pv) * max)
    else if (0 <= sp.abs && sp.abs < pv.abs)
      if (brakingPatterns
      (((sp.abs / pv.abs) * brakingPatterns.length).toInt)
      (tick.abs % ditherLength)
      ) None
      else Some(Volts(0))
    else Some(target)
  }
}
