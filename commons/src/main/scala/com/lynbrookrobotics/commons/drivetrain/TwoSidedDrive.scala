package com.lynbrookrobotics.commons.drivetrain

import com.lynbrookrobotics.potassium.{Component, PeriodicSignal, Signal}
import squants.{Dimensionless, Time}
import squants.electro.ElectricPotential

trait TwoSidedDrive extends UnicycleDrive {
  case class DriveSignal(leftPower: ElectricPotential, rightPower: ElectricPotential)
  override def addDriveSignal(a: DriveSignal, b: DriveSignal): DriveSignal =
    DriveSignal(a.leftPower + b.leftPower, a.rightPower + b.rightPower)

  val updatePeriod: Time
  val motorVoltageScale: ElectricPotential

  def outputLeft(left: ElectricPotential): Unit
  def outputRight(right: ElectricPotential): Unit

  override def forwardControl(forwardSpeed: Signal[Dimensionless]): Signal[DriveSignal] = {
    forwardSpeed.map(s => DriveSignal(s.toEach * motorVoltageScale, s.toEach * motorVoltageScale))
  }

  override def turnControl(turnSpeed: Signal[Dimensionless]): Signal[DriveSignal] = {
    turnSpeed.map(s => DriveSignal(s.toEach * motorVoltageScale, -s.toEach * motorVoltageScale))
  }

  object Drivetrain extends Component[DriveSignal](updatePeriod) {
    override def defaultController: PeriodicSignal[DriveSignal] =
      Signal.constant(DriveSignal(0 * motorVoltageScale, 0 * motorVoltageScale)).toPeriodic

    override def applySignal(signal: DriveSignal): Unit = {
      outputLeft(signal.leftPower)
      outputRight(signal.rightPower)
    }
  }
}
