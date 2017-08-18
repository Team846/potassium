package com.lynbrookrobotics.potassium.commons.drivetrain
import com.lynbrookrobotics.potassium.streams.Stream

import squants.Dimensionless

trait UnicycleControlMode

case object NoOperation extends UnicycleControlMode

case class ArcadeControlsOpen(forward: Stream[Dimensionless], turn: Stream[Dimensionless]) extends UnicycleControlMode

case class ArcadeControlsClosed(forward: Stream[Dimensionless], turn: Stream[Dimensionless]) extends UnicycleControlMode
