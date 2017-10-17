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
    ".|.|.||.|.||",
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

  def dynamicBrakingOutput(max: ElectricPotential, free: AngularVelocity)
                          (tick: Int, target: ElectricPotential, current: AngularVelocity)
  : Option[ElectricPotential] = {
    // normalized
    val nTarget = target / max
    val nSpeed = current / free

    if ((nSpeed < 0) != (nTarget < 0)) Some((nTarget - nSpeed) * max)
    else if (0 <= nTarget.abs && nTarget.abs < nSpeed.abs)
      if (brakingPatterns
      (((nTarget.abs / nSpeed.abs) * brakingPatterns.length).toInt)
      (tick.abs % ditherLength)
      ) None
      else Some(Volts(0))
    else Some(target)
  }
}
