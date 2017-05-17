package com.lynbrookrobotics.potassium.commons.drivetrain

import com.lynbrookrobotics.potassium.clock.Clock
import com.lynbrookrobotics.potassium.control.{PIDConfig, PIDF}
import com.lynbrookrobotics.potassium.units._
import com.lynbrookrobotics.potassium.{Component, PeriodicSignal, Signal, SignalLike}
import squants.motion.{AngularVelocity, MetersPerSecond, MetersPerSecondSquared, RadiansPerSecond}
import squants.space.{Meters, Radians}
import squants.time.Seconds
import squants.{Angle, Dimensionless, Each, Length, Percent, Time, Velocity}

trait TwoSidedDriveHardware extends UnicycleHardware {
  val leftVelocity: Signal[Velocity]
  val rightVelocity: Signal[Velocity]

  val leftPosition: Signal[Length]
  val rightPosition: Signal[Length]

  val track: Length

  lazy val forwardVelocity: Signal[Velocity] =
    leftVelocity.zip(rightVelocity).map(t => (t._1 + t._2) / 2)

  lazy val turnVelocity: Signal[AngularVelocity] = {
    rightVelocity.zip(leftVelocity).map { case (r, l) =>
      RadiansPerSecond(((l - r) * Seconds(1)) / track)
    }
  }

  lazy val forwardPosition: Signal[Length] =
    leftPosition.zip(rightPosition).map(t => (t._1 + t._2) / 2)

  lazy val turnPosition: Signal[Angle] = leftPosition.zip(rightPosition).map(t =>
    Radians((t._1 - t._2) / track)
  )
}

trait TwoSidedDriveProperties extends UnicycleProperties {
  val maxLeftVelocity: Velocity
  val maxRightVelocity: Velocity

  val leftControlGains: ForwardVelocityGains
  val rightControlGains: ForwardVelocityGains

  lazy val leftControlGainsFull: ForwardVelocityGains#Full =
    leftControlGains.withF(Percent(100) / maxLeftVelocity)
  lazy val rightControlGainsFull: ForwardVelocityGains#Full =
    rightControlGains.withF(Percent(100) / maxRightVelocity)

  lazy val maxForwardVelocity: Velocity = maxLeftVelocity min maxRightVelocity

  val forwardControlGains: ForwardVelocityGains = PIDConfig(
    Each(0) / MetersPerSecond(1),
    Each(0) / Meters(1),
    Each(0) / MetersPerSecondSquared(1)
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
        props.map(_.leftControlGainsFull)
      )

      val rightControl = PIDF.pidf(
        rightVelocity.toPeriodic,
        target.map(_.right).toPeriodic,
        props.map(_.rightControlGainsFull)
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
