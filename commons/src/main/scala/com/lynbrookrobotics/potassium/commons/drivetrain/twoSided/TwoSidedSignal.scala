package com.lynbrookrobotics.potassium.commons.drivetrain.twoSided

import squants.{Dimensionless, Velocity}

case class TwoSidedSignal(left: Dimensionless, right: Dimensionless)

case class TwoSidedVelocity(left: Velocity, right: Velocity)
