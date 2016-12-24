package com.lynbrookrobotics.potassium.commons.drivetrain

import com.lynbrookrobotics.potassium.Signal
import squants.Dimensionless

trait UnicycleControlMode

case object NoOperation extends UnicycleControlMode

case class ArcadeControls(forward: Signal[Dimensionless], turn: Signal[Dimensionless]) extends UnicycleControlMode
