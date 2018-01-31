package com.lynbrookrobotics.potassium.commons.lift.offloaded

import com.lynbrookrobotics.potassium.commons.lift.LiftProperties
import com.lynbrookrobotics.potassium.control.offload.EscConfig
import squants.space.Length

trait OffloadedProperties extends LiftProperties {
  val escConfig: EscConfig[Length]
}
