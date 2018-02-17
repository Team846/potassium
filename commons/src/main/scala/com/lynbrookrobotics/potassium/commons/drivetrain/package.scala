package com.lynbrookrobotics.potassium.commons

import com.lynbrookrobotics.potassium.control.{PIDConfig, PIDFConfig, PIDProperUnitsConfig}
import com.lynbrookrobotics.potassium.units.{GenericDerivative, GenericIntegral, GenericValue}
import squants.motion.AngularVelocity
import squants.{Acceleration, Angle, Dimensionless, Length, Velocity}

package object drivetrain {
  type ForwardVelocityGains = PIDProperUnitsConfig[Velocity, Acceleration, Length, Dimensionless]
  type TurnVelocityGains = PIDConfig[AngularVelocity,
                                     GenericValue[AngularVelocity],
                                     AngularVelocity,
                                     GenericDerivative[AngularVelocity],
                                     Angle,
                                     Dimensionless]

  type ForwardPositionGains = PIDConfig[Length,
                                        Length,
                                        GenericValue[Length],
                                        Velocity,
                                        GenericIntegral[Length],
                                        Dimensionless]

  type TurnPositionGains = PIDConfig[Angle,
                                     Angle,
                                     GenericValue[Angle],
                                     AngularVelocity,
                                     GenericIntegral[Angle],
                                     Dimensionless]
}
