package com.lynbrookrobotics.commons.drivetrain

import com.lynbrookrobotics.potassium.Signal
import squants.Dimensionless
import squants.electro.ElectricPotential

trait TwoSidedDrive extends UnicycleDrive {
  case class DriveSignal(leftPower: ElectricPotential, rightPower: ElectricPotential)

  override def addDriveSignal(a: DriveSignal, b: DriveSignal): DriveSignal =
    DriveSignal(a.leftPower + b.leftPower, a.rightPower + b.rightPower)

  val motorVoltageScale: ElectricPotential

  override def forwardControl(forwardSpeed: Signal[Dimensionless]): Signal[DriveSignal] = {
    forwardSpeed.map(s => DriveSignal(s.toEach * motorVoltageScale, s.toEach * motorVoltageScale))
  }

  override def turnControl(turnSpeed: Signal[Dimensionless]): Signal[DriveSignal] = {
    turnSpeed.map(s => DriveSignal(s.toEach * motorVoltageScale, -s.toEach * motorVoltageScale))
  }
}
