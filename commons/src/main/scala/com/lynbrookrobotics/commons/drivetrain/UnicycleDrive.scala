package com.lynbrookrobotics.commons.drivetrain

import com.lynbrookrobotics.potassium.{Component, Signal}
import squants.Dimensionless

trait UnicycleDrive {
  type DriveSignal

  def addDriveSignal(a: DriveSignal, b: DriveSignal): DriveSignal

  def forwardControl(forwardSpeed: Signal[Dimensionless]): Signal[DriveSignal]
  def turnControl(turnSpeed: Signal[Dimensionless]): Signal[DriveSignal]

  def forwardAndTurnControl(forwardSpeed: Signal[Dimensionless], turnSpeed: Signal[Dimensionless]): Signal[DriveSignal] = {
    forwardControl(forwardSpeed).zip(turnControl(turnSpeed)).map(t => addDriveSignal(t._1, t._2))
  }

  type Drivetrain <: Component[DriveSignal]
}
