package com.lynbrookrobotics.potassium.commons.drivetrain.offloaded

import com.lynbrookrobotics.potassium.commons.drivetrain.twoSided.TwoSidedDriveProperties
import com.lynbrookrobotics.potassium.control.offload.EscConfig
import com.lynbrookrobotics.potassium.units.Ratio
import squants.space.{Degrees, Length}
import squants.{Angle, Dimensionless}

trait OffloadedProperties extends TwoSidedDriveProperties {
  val escConfig: EscConfig[Length]

  val wheelDiameter: Length
  val wheelOverEncoderGears: Ratio[Angle, Angle]
  val encoderAngleOverTicks: Ratio[Angle, Dimensionless]

  lazy val floorPerTick: Ratio[Length, Dimensionless] =
    Ratio(wheelDiameter * Math.PI, Degrees(360)) * wheelOverEncoderGears * encoderAngleOverTicks
}