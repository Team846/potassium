package com.lynbrookrobotics.commons.drivetrain

import com.lynbrookrobotics.potassium.clock.Clock
import com.lynbrookrobotics.potassium.units._
import com.lynbrookrobotics.potassium.{Component, PeriodicSignal, Signal}
import squants.{Dimensionless, Each, Time, Velocity}
import squants.electro.ElectricPotential
import squants.motion.MetersPerSecond

/**
  * A drivetrain with two side control (such as a tank drive)
  */
trait TwoSidedDrive extends UnicycleDrive { self =>
  case class TwoSidedSignal(left: Dimensionless, right: Dimensionless)
  case class TwoSidedVelocity(left: Velocity, right: Velocity)

  type DriveSignal = TwoSidedSignal
  type DriveVelocity = TwoSidedVelocity

  protected implicit val clock: Clock
  protected val updatePeriod: Time

  protected def output(signal: TwoSidedSignal): Unit

  protected def convertUnicycleToDrive(uni: UnicycleSignal): TwoSidedSignal = {
    TwoSidedSignal(
      uni.forward + uni.turn,
      uni.forward - uni.turn
    )
  }

  protected def expectedVelocity(drive: TwoSidedSignal): TwoSidedVelocity = {
    TwoSidedVelocity(maxLeftVelocity * drive.left, maxRightVelocity * drive.right)
  }

  protected def driveVelocityControl(signal: Signal[TwoSidedVelocity]): Signal[TwoSidedSignal] =
    TwoSidedControllers.velocity(signal)

  protected def driveClosedLoop(signal: Signal[TwoSidedSignal]): Signal[TwoSidedSignal] =
    TwoSidedControllers.velocity(signal.map(expectedVelocity))

  protected val maxLeftVelocity: Velocity
  protected val maxRightVelocity: Velocity

  // TODO: replace with configuration object from control module
  protected val leftProportionalGain: Ratio[Dimensionless, Velocity]
  protected val rightProportionalGain: Ratio[Dimensionless, Velocity]

  protected val leftFeedForwardGain: Ratio[Dimensionless, Velocity]
  protected val rightFeedForwardGain: Ratio[Dimensionless, Velocity]

  // disable unicycle forward control because we already control that here
  override protected val forwardProportionalGain: Ratio[Dimensionless, Velocity] =
    Each(0) / MetersPerSecond(1)

  protected val leftVelocity: Signal[Velocity]
  protected val rightVelocity: Signal[Velocity]

  override protected val forwardVelocity: Signal[Velocity] =
    leftVelocity.zip(rightVelocity).map(t => (t._1 + t._2) / 2)

  object TwoSidedControllers {
    def velocity(twoSided: Signal[TwoSidedVelocity]): Signal[TwoSidedSignal] = {
      twoSided.zip(leftVelocity).zip(rightVelocity).map { case ((target, curLeft), curRight) =>
        TwoSidedSignal(
          (target.left ** leftFeedForwardGain) + ((target.left - curLeft) ** leftProportionalGain),
          (target.right ** rightFeedForwardGain) + ((target.right - curRight) ** rightProportionalGain)
        )
      }
    }
  }

  class Drivetrain extends Component[TwoSidedSignal](updatePeriod) {
    override def defaultController: PeriodicSignal[TwoSidedSignal] = self.defaultController

    override def applySignal(signal: TwoSidedSignal): Unit = {
      output(signal)
    }
  }
}
