package com.lynbrookrobotics.potassium.commons.drivetrain

import com.lynbrookrobotics.potassium.control.PIDF
import com.lynbrookrobotics.potassium.{PeriodicSignal, Signal, SignalLike}
import squants.{Angle, Dimensionless, Length, Percent}
import com.lynbrookrobotics.potassium.units._

trait UnicycleCoreControllers {
  type DriveSignal
  type DrivetrainHardware <: UnicycleHardware
  type DrivetrainProperties <: UnicycleProperties

  def parentOpenLoop(unicycle: SignalLike[UnicycleSignal]): PeriodicSignal[DriveSignal]

  def lowerLevelVelocityControl(unicycle: SignalLike[UnicycleSignal])(implicit hardware: DrivetrainHardware,
                                                                      props: Signal[DrivetrainProperties]): PeriodicSignal[DriveSignal]

  def openForwardOpenDrive(forwardSpeed: Signal[Dimensionless]): PeriodicSignal[DriveSignal] = {
    parentOpenLoop(forwardSpeed.map(f => UnicycleSignal(f, Percent(0))))
  }

  def openForwardClosedDrive(forwardSpeed: Signal[Dimensionless])(implicit hardware: DrivetrainHardware,
                                                                  props: Signal[DrivetrainProperties]): PeriodicSignal[DriveSignal] = {
    lowerLevelVelocityControl(forwardSpeed.map(f => UnicycleSignal(f, Percent(0))))
  }

  def openTurnOpenDrive(turnSpeed: Signal[Dimensionless]): PeriodicSignal[DriveSignal] = {
    parentOpenLoop(turnSpeed.map(t => UnicycleSignal(Percent(0), t)))
  }

  def openTurnClosedDrive(turnSpeed: Signal[Dimensionless])(implicit hardware: DrivetrainHardware,
                                                            props: Signal[DrivetrainProperties]): PeriodicSignal[DriveSignal] = {
    lowerLevelVelocityControl(turnSpeed.map(t => UnicycleSignal(Percent(0), t)))
  }

  def velocityControl(target: SignalLike[UnicycleVelocity])
                     (implicit hardware: DrivetrainHardware,
                      props: Signal[DrivetrainProperties]): PeriodicSignal[UnicycleSignal] = {
    import hardware._

    val forwardControl = PIDF.pidf(
      forwardVelocity.toPeriodic,
      target.map(_.forward).toPeriodic,
      props.map(_.forwardControlGainsFull)
    )

    val turnControl = PIDF.pidf(
      turnVelocity.toPeriodic,
      target.map(_.turn).toPeriodic,
      props.map(_.turnControlGainsFull)
    )

    forwardControl.zip(turnControl).map(s => UnicycleSignal(s._1, s._2))
  }

  def speedControl(unicycle: SignalLike[UnicycleSignal])
                       (implicit hardware: DrivetrainHardware,
                        props: Signal[DrivetrainProperties]): PeriodicSignal[UnicycleSignal] = {
    velocityControl(unicycle.map(s => UnicycleVelocity(
      props.get.maxForwardVelocity * s.forward, props.get.maxTurnVelocity * s.turn
    )))
  }

  def forwardPositionControl(targetAbsolute: Length)
                            (implicit hardware: DrivetrainHardware,
                             props: Signal[DrivetrainProperties]): (PeriodicSignal[UnicycleSignal], Signal[Length]) = {
    val error = hardware.forwardPosition.map(targetAbsolute - _)

    val control = PIDF.pid(
      hardware.forwardPosition.toPeriodic,
      Signal.constant(targetAbsolute).toPeriodic,
      props.map(_.forwardPositionControlGains)
    ).map(s => UnicycleSignal(s, Percent(0)))

    (control, error)
  }

  def continuousTurnPositionControl(targetAbsolute: Signal[Angle])
    (implicit hardware: DrivetrainHardware,
      props: Signal[DrivetrainProperties]): (PeriodicSignal[UnicycleSignal], Signal[Angle]) = {
    val error = hardware.turnPosition.map(targetAbsolute.get - _)

    val control = PIDF.pid(
      hardware.turnPosition.toPeriodic,
      targetAbsolute.toPeriodic,
      props.map(_.turnPositionControlGains)
    ).map(s => UnicycleSignal(Percent(0), s))

    (control, error)
  }

  def turnPositionControl(targetAbsolute: Angle)
                         (implicit hardware: DrivetrainHardware,
                          props: Signal[DrivetrainProperties]): (PeriodicSignal[UnicycleSignal], Signal[Angle]) = {
    continuousTurnPositionControl(Signal.constant(targetAbsolute))
  }
}
