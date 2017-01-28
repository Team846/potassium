package com.lynbrookrobotics.potassium.commons.drivetrain

import com.lynbrookrobotics.potassium.clock.Clock
import com.lynbrookrobotics.potassium.control.{PIDF, PIDFConfig}
import com.lynbrookrobotics.potassium.units._
import com.lynbrookrobotics.potassium.{Component, PeriodicSignal, Signal, SignalLike}

import squants.motion.{MetersPerSecond, MetersPerSecondSquared}
import squants.space.Meters
import squants.{Dimensionless, Each, Time, Velocity}

trait TwoSidedDriveHardware extends UnicycleHardware {
  val leftVelocity: Signal[Velocity]
  val rightVelocity: Signal[Velocity]

  val forwardVelocity: Signal[Velocity] =
    leftVelocity.zip(rightVelocity).map(t => (t._1 + t._2) / 2)
}

trait TwoSidedDriveProperties extends UnicycleProperties {
  val maxLeftVelocity: Velocity
  val maxRightVelocity: Velocity
  val leftControlGains: VelocityGains
  val rightControlGains: VelocityGains

  val maxForwardVelocity: Velocity = maxLeftVelocity min maxRightVelocity

  val forwardControlGains: VelocityGains = PIDFConfig(
    Each(0) / MetersPerSecond(1),
    Each(0) / MetersPerSecondSquared(1),
    Each(0) / Meters(1),
    Each(1) / maxForwardVelocity
  )
}

/**
  * A drivetrain with two side control (such as a tank drive)
  */
trait TwoSidedDrive extends UnicycleDrive { self =>
  case class TwoSidedSignal(left: Dimensionless, right: Dimensionless)
  case class TwoSidedVelocity(left: Velocity, right: Velocity)

  type DriveSignal = TwoSidedSignal
  type DriveVelocity = TwoSidedVelocity

  type DrivetrainHardware <: TwoSidedDriveHardware
  type DrivetrainProperties <: TwoSidedDriveProperties

  protected implicit val clock: Clock
  protected val updatePeriod: Time

  //TODO: add docs
  protected def output(hardware: DrivetrainHardware, signal: TwoSidedSignal): Unit

  protected def convertUnicycleToDrive(uni: UnicycleSignal): TwoSidedSignal = {
    TwoSidedSignal(
      uni.forward + uni.turn,
      uni.forward - uni.turn
    )
  }

  protected def expectedVelocity(drive: TwoSidedSignal)(implicit props: DrivetrainProperties): TwoSidedVelocity = {
    TwoSidedVelocity(props.maxLeftVelocity * drive.left, props.maxRightVelocity * drive.right)
  }

  protected def driveClosedLoop(signal: SignalLike[TwoSidedSignal])
                               (implicit hardware: DrivetrainHardware,
                                         props: DrivetrainProperties): PeriodicSignal[TwoSidedSignal] =
    TwoSidedControllers.closedLoopControl(signal)

  object TwoSidedControllers {
    def velocityControl(target: SignalLike[TwoSidedVelocity])
                       (implicit hardware: DrivetrainHardware,
                                 props: DrivetrainProperties): PeriodicSignal[TwoSidedSignal] = {
      import hardware._
      import props._

      val leftControl = PIDF.pidf(
        leftVelocity.toPeriodic,
        target.map(_.left).toPeriodic,
        leftControlGains
      )

      val rightControl = PIDF.pidf(
        rightVelocity.toPeriodic,
        target.map(_.right).toPeriodic,
        rightControlGains
      )

      leftControl.zip(rightControl).map(s => TwoSidedSignal(s._1, s._2))
    }

    def closedLoopControl(signal: SignalLike[TwoSidedSignal])
                         (implicit hardware: DrivetrainHardware,
                                   props: DrivetrainProperties): PeriodicSignal[TwoSidedSignal] = {
      velocityControl(signal.map(expectedVelocity))
    }
  }

  class Drivetrain(implicit hardware: DrivetrainHardware, props: DrivetrainProperties) extends Component[TwoSidedSignal](updatePeriod) {
    override def defaultController: PeriodicSignal[TwoSidedSignal] = self.defaultController

    override def applySignal(signal: TwoSidedSignal): Unit = {
      output(hardware, signal)
    }
  }
}
