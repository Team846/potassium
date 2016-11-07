package com.lynbrookrobotics.commons.drivetrain

import com.lynbrookrobotics.potassium.{Component, PeriodicSignal}
import squants.Time
import squants.electro.ElectricPotential

/**
  * A drivetrain with two side control (such as a tank drive)
  */
trait TwoSidedDrive extends UnicycleDrive {
  case class DriveSignal(leftPower: ElectricPotential, rightPower: ElectricPotential)

  protected val updatePeriod: Time
  protected val motorVoltageScale: ElectricPotential

  protected def outputLeft(left: ElectricPotential): Unit
  protected def outputRight(right: ElectricPotential): Unit

  protected def convertToDrive(uni: UnicycleSignal): DriveSignal = {
    DriveSignal(
      motorVoltageScale * (uni.forward + uni.turn).toEach,
      motorVoltageScale * (uni.forward - uni.turn).toEach
    )
  }

  class Drivetrain extends Component[DriveSignal](updatePeriod) {
    override def defaultController: PeriodicSignal[DriveSignal] = defaultController

    override def applySignal(signal: DriveSignal): Unit = {
      outputLeft(signal.leftPower)
      outputRight(signal.rightPower)
    }
  }
}
