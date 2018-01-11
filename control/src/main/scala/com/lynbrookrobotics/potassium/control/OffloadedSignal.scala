package com.lynbrookrobotics.potassium.control

import com.lynbrookrobotics.potassium.control.OffloadedSignal.{PositionGains, VelocityGains}
import com.lynbrookrobotics.potassium.units.{GenericIntegral, GenericValue}
import squants.motion.{AngularAcceleration, AngularVelocity}
import squants.{Angle, Dimensionless, Ratio}

sealed class OffloadedSignal

object OffloadedSignal {
  type VelocityGains = PIDConfig[AngularVelocity,
    GenericValue[AngularVelocity],
    AngularVelocity,
    AngularAcceleration,
    Angle,
    Dimensionless]

  type PositionGains = PIDConfig[Angle,
    Angle,
    GenericValue[Angle],
    AngularVelocity,
    GenericIntegral[Angle],
    Dimensionless]
}

case class VelocityControl(encoderTickConversionFactor: Ratio[Angle, Int])
                          (gains: VelocityGains)
                          (signal: AngularVelocity) extends OffloadedSignal

case class PositionControl(encoderTickConversionFactor: Ratio[Angle, Int])
                          (gains: PositionGains)
                          (signal: Angle) extends OffloadedSignal

case class OpenLoop(signal: Dimensionless) extends OffloadedSignal