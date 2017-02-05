package com.lynbrookrobotics.potassium.commons.drivetrain

import com.lynbrookrobotics.potassium.clock.Clock
import com.lynbrookrobotics.potassium.control.{PIDF, PIDFConfig}
import com.lynbrookrobotics.potassium.units._
import com.lynbrookrobotics.potassium.{Component, PeriodicSignal, Signal, SignalLike}

import squants.motion.{MetersPerSecond, MetersPerSecondSquared}
import squants.space.Meters
import squants.{Dimensionless, Each, Time, Velocity}

trait TwoSidedDriveHardware extends UnicycleHardware {
  def leftVelocity: Signal[Velocity]
  def rightVelocity: Signal[Velocity]

  lazy val forwardVelocity: Signal[Velocity] =
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
    Each(0) / Meters(1),
    Each(0) / MetersPerSecondSquared(1),
    Each(1) / maxForwardVelocity
  )
}

/**
  * A drivetrain with two side control (such as a tank drive)
  */
abstract class TwoSidedDrive(updatePeriod: Time)(implicit clock: Clock)
  extends UnicycleDrive { self =>
  case class TwoSidedSignal(left: Dimensionless, right: Dimensionless)
  case class TwoSidedVelocity(left: Velocity, right: Velocity)

  type DriveSignal = TwoSidedSignal
  type DriveVelocity = TwoSidedVelocity

  type Hardware <: TwoSidedDriveHardware
  type Properties <: TwoSidedDriveProperties

  /**
    * Output the current signal to actuators with the hardware
    * @param hardware the hardware to output with
    * @param signal the signal to output
    */
  protected def output(hardware: Hardware, signal: TwoSidedSignal): Unit

  protected def convertUnicycleToDrive(uni: UnicycleSignal): TwoSidedSignal = {
    TwoSidedSignal(
      uni.forward + uni.turn,
      uni.forward - uni.turn
    )
  }

  protected def expectedVelocity(drive: TwoSidedSignal)(implicit props: Properties): TwoSidedVelocity = {
    TwoSidedVelocity(props.maxLeftVelocity * drive.left, props.maxRightVelocity * drive.right)
  }

  protected def driveClosedLoop(signal: SignalLike[TwoSidedSignal])
                               (implicit hardware: Hardware,
                                props: Signal[Properties]): PeriodicSignal[TwoSidedSignal] =
    TwoSidedControllers.closedLoopControl(signal)

  object TwoSidedControllers {
    def velocityControl(target: SignalLike[TwoSidedVelocity])
                       (implicit hardware: Hardware,
                        props: Signal[Properties]): PeriodicSignal[TwoSidedSignal] = {
      import hardware._

      val leftControl = PIDF.pidf(
        leftVelocity.toPeriodic,
        target.map(_.left).toPeriodic,
        props.map(_.leftControlGains)
      )

      val rightControl = PIDF.pidf(
        rightVelocity.toPeriodic,
        target.map(_.right).toPeriodic,
        props.map(_.rightControlGains)
      )

      leftControl.zip(rightControl).map(s => TwoSidedSignal(s._1, s._2))
    }

    def closedLoopControl(signal: SignalLike[TwoSidedSignal])
                         (implicit hardware: Hardware,
                          props: Signal[Properties]): PeriodicSignal[TwoSidedSignal] = {
      velocityControl(signal.map(s => expectedVelocity(s)(props.get)))
    }
  }

  class Drivetrain(implicit hardware: Hardware, props: Signal[Properties]) extends Component[TwoSidedSignal](updatePeriod) {
    override def defaultController: PeriodicSignal[TwoSidedSignal] = self.defaultController

    override def applySignal(signal: TwoSidedSignal): Unit = {
      output(hardware, signal)
    }
  }
}
