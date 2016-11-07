package com.lynbrookrobotics.commons.drivetrain

import com.lynbrookrobotics.potassium.{Component, PeriodicSignal}

/**
  * Represents a drivetrain container with a signal type, default controller, and component
  */
trait Drive {
  type DriveSignal

  protected val defaultController: PeriodicSignal[DriveSignal]

  type Drivetrain <: Component[DriveSignal]
}
