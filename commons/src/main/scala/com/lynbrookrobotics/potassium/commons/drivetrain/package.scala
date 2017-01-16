package com.lynbrookrobotics.potassium.commons

import com.lynbrookrobotics.potassium.control.{PIDFConfig, PIDFProperUnitsConfig}
import com.lynbrookrobotics.potassium.units.{GenericDerivative, GenericIntegral, GenericValue}

import squants.motion.AngularVelocity
import squants.{Acceleration, Dimensionless, Length, Velocity}

package object drivetrain {
  type VelocityGains = PIDFProperUnitsConfig[Velocity, Acceleration, Length, Dimensionless]

  type AngularVelocityGains = PIDFConfig[AngularVelocity,
                                         GenericValue[AngularVelocity],
                                         GenericDerivative[AngularVelocity],
                                         GenericIntegral[AngularVelocity],
                                         Dimensionless]
}
