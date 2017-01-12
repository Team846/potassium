package com.lynbrookrobotics.potassium.commons.drivetrain

import com.lynbrookrobotics.potassium.clock.Clock
import com.lynbrookrobotics.potassium.control.{PIDF, PIDFConfig, PIDFProperUnitsConfig}
import com.lynbrookrobotics.potassium.units._
import com.lynbrookrobotics.potassium.{Component, PeriodicSignal, Signal, SignalLike}
import squants.{Acceleration, Dimensionless, Each, Length, Time, Velocity}
import squants.motion.{MetersPerSecond, MetersPerSecondSquared}
import squants.space.Meters

/**
  * A drivetrain with two side control (such as a tank drive)
  */
trait TwoSidedDrive extends UnicycleDrive { self =>
  case class TwoSidedSignal(left: Dimensionless, right: Dimensionless)
  case class TwoSidedVelocity(left: Velocity, right: Velocity)

  type DriveSignal = TwoSidedSignal
  type DriveVelocity = TwoSidedVelocity

  type DrivetrainHardware

  protected implicit val clock: Clock
  protected val updatePeriod: Time

  protected def output(hardware: DrivetrainHardware, signal: TwoSidedSignal): Unit

  protected def convertUnicycleToDrive(uni: UnicycleSignal): TwoSidedSignal = {
    TwoSidedSignal(
      uni.forward + uni.turn,
      uni.forward - uni.turn
    )
  }

  protected def expectedVelocity(drive: TwoSidedSignal): TwoSidedVelocity = {
    TwoSidedVelocity(maxLeftVelocity * drive.left, maxRightVelocity * drive.right)
  }

  protected def driveClosedLoop[C[_]](signal: SignalLike[TwoSidedSignal, C]): PeriodicSignal[TwoSidedSignal] =
    TwoSidedControllers.velocity(signal.map(expectedVelocity))

  protected def driveVelocity[C[_]](signal: SignalLike[TwoSidedVelocity, C]): PeriodicSignal[TwoSidedSignal] =
    TwoSidedControllers.velocity(signal)

  protected val maxLeftVelocity: Velocity
  protected val maxRightVelocity: Velocity

  protected val leftControlGains: PIDFProperUnitsConfig[Velocity, Acceleration, Length, Dimensionless]
  protected val rightControlGains: PIDFProperUnitsConfig[Velocity, Acceleration, Length, Dimensionless]

  // disable unicycle forward control because we already control that here
  override protected val forwardControlGains: PIDFConfig[Velocity, Velocity, Acceleration, Length, Dimensionless] = PIDFConfig(
    Each(0) / MetersPerSecond(1),
    Each(0) / MetersPerSecondSquared(1),
    Each(0) / Meters(1),
    Each(0) / MetersPerSecond(0)
  )

  protected val leftVelocity: Signal[Velocity]
  protected val rightVelocity: Signal[Velocity]

  override protected val forwardVelocity: Signal[Velocity] =
    leftVelocity.zip(rightVelocity).map(t => (t._1 + t._2) / 2)

  object TwoSidedControllers {
    def velocity[C[_]](target: SignalLike[TwoSidedVelocity, C]): PeriodicSignal[TwoSidedSignal] = {
      val leftControl = PIDF.pidf(leftVelocity.toPeriodic, target.map(_.left).toPeriodic, leftControlGains)
      val rightControl = PIDF.pidf(rightVelocity.toPeriodic, target.map(_.right).toPeriodic, rightControlGains)

      leftControl.zip(rightControl).map((s, _) => TwoSidedSignal(s._1, s._2))
    }
  }

  class Drivetrain(implicit hardware: DrivetrainHardware) extends Component[TwoSidedSignal](updatePeriod) {
    override def defaultController: PeriodicSignal[TwoSidedSignal] = self.defaultController

    override def applySignal(signal: TwoSidedSignal): Unit = {
      output(hardware, signal)
    }
  }
}
