package com.lynbrookrobotics.potassium.units.rotation

import squants.Energy

object Conversions {
  implicit def fromEnergyToTorque(energy: Energy): Torque = {
    NewtonMeters(energy.toJoules)
  }
}
