package com.lynbrookrobotics.potassium.commons.lift.offloaded

import com.lynbrookrobotics.potassium.commons.lift.LiftProperties
import com.lynbrookrobotics.potassium.control.offload.EscConfig
import squants.Dimensionless
import squants.space.Length

trait OffloadedLiftProperties extends LiftProperties {
  val escConfig: EscConfig[Length]
  def toNative(height: Length): Dimensionless
  def fromNative(native: Dimensionless): Length
}
