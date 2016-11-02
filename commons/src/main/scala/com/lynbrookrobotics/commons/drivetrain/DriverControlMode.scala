package com.lynbrookrobotics.commons.drivetrain

import com.lynbrookrobotics.potassium.Signal
import squants.Dimensionless

trait DriverControlMode

case object NoOperation extends DriverControlMode

case class ArcadeControls(forward: Signal[Dimensionless], turn: Signal[Dimensionless]) extends DriverControlMode
