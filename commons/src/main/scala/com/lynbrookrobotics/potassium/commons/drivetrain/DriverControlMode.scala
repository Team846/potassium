package com.lynbrookrobotics.potassium.commons.drivetrain

import com.lynbrookrobotics.potassium.Signal
import squants.Dimensionless

trait UnicycleControlMode

case object NoOperation extends UnicycleControlMode

case class ArcadeControlsOpen(forward: Signal[Dimensionless], turn: Signal[Dimensionless]) extends UnicycleControlMode

case class ArcadeControlsClosed(forward: Signal[Dimensionless], turn: Signal[Dimensionless]) extends UnicycleControlMode
