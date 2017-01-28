package com.lynbrookrobotics.potassium.commons.drivetrain

import com.lynbrookrobotics.potassium.control.PIDF
import com.lynbrookrobotics.potassium.{PeriodicSignal, Signal, SignalLike}
import squants.{Angle, Dimensionless, Length, Percent}
import com.lynbrookrobotics.potassium.units._

trait UnicycleCoreControllers {
  type DriveSignal
  type DrivetrainHardware <: UnicycleHardware
  type DrivetrainProperties <: UnicycleProperties

  def parentClosedLoop(unicycle: SignalLike[UnicycleSignal]): PeriodicSignal[DriveSignal]
  def openForwardClosedDrive(forwardSpeed: Signal[Dimensionless]): PeriodicSignal[DriveSignal] = {
    parentClosedLoop(forwardSpeed.map(f => UnicycleSignal(f, Percent(0))))
  }

  def openTurnClosedDrive(turnSpeed: Signal[Dimensionless]): PeriodicSignal[DriveSignal] = {
    parentClosedLoop(turnSpeed.map(t => UnicycleSignal(Percent(0), t)))
  }

  def velocityControl(target: SignalLike[UnicycleVelocity])
                     (implicit hardware: DrivetrainHardware,
                      props: DrivetrainProperties): PeriodicSignal[UnicycleSignal] = {
    import hardware._
    import props._

    val forwardControl = PIDF.pidf(
      forwardVelocity.toPeriodic,
      target.map(_.forward).toPeriodic,
      forwardControlGains
    )

    val turnControl = PIDF.pidf(
      turnVelocity.toPeriodic,
      target.map(_.turn).toPeriodic,
      turnControlGains
    )

    forwardControl.zip(turnControl).map(s => UnicycleSignal(s._1, s._2))
  }

  def speedControl(unicycle: SignalLike[UnicycleSignal])
                       (implicit hardware: DrivetrainHardware,
                        props: DrivetrainProperties): PeriodicSignal[UnicycleSignal] = {
    import props._

    velocityControl(unicycle.map(s => UnicycleVelocity(
      maxForwardVelocity * s.forward, maxTurnVelocity * s.turn
    )))
  }

  def forwardPositionControl(targetAbsolute: Length)
                            (implicit hardware: DrivetrainHardware,
                             props: DrivetrainProperties): (PeriodicSignal[UnicycleSignal], Signal[Length]) = {
    val error = hardware.forwardPosition.map(targetAbsolute - _)

    val control = PIDF.pidf(
      hardware.forwardPosition.toPeriodic,
      Signal.constant(targetAbsolute).toPeriodic,
      props.forwardPositionControlGains
    ).map(s => UnicycleSignal(s, Percent(0)))

    (control, error)
  }

  def turnPositionControl(targetAbsolute: Angle)
                         (implicit hardware: DrivetrainHardware,
                          props: DrivetrainProperties): (PeriodicSignal[UnicycleSignal], Signal[Angle]) = {
    val error = hardware.turnPosition.map(targetAbsolute - _)

    val control = PIDF.pidf(
      hardware.turnPosition.toPeriodic,
      Signal.constant(targetAbsolute).toPeriodic,
      props.turnPositionControlGains
    ).map(s => UnicycleSignal(Percent(0), s))

    (control, error)
  }
}
