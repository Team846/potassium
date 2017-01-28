package com.lynbrookrobotics.potassium.commons

import com.lynbrookrobotics.potassium.control.{PIDFConfig, PIDFProperUnitsConfig}
import com.lynbrookrobotics.potassium.units.{GenericDerivative, GenericValue}
import squants.motion.AngularVelocity
import squants.{Acceleration, Angle, Dimensionless, Length, Velocity}

package object drivetrain {
  type VelocityGains = PIDFProperUnitsConfig[Velocity, Acceleration, Length, Dimensionless]

  type AngularVelocityGains = PIDFConfig[AngularVelocity,
                                         GenericValue[AngularVelocity],
                                         AngularVelocity,
                                         GenericDerivative[AngularVelocity],
                                         Angle,
                                         Dimensionless]
}
