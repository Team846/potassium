package com.lynbrookrobotics.potassium.commons.drivetrain
import com.lynbrookrobotics.potassium.control.PIDF
import com.lynbrookrobotics.potassium.{SignalLike, PeriodicSignal, Signal}
import squants.{Percent, Dimensionless}

import com.lynbrookrobotics.potassium.units._

trait UnicycleCoreControllers[DriveSignal, DrivetrainHardware <: UnicycleHardware, DrivetrainProperties <: UnicycleProperties] {
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

  def closedLoopControl(unicycle: SignalLike[UnicycleSignal])
                       (implicit hardware: DrivetrainHardware,
                        props: DrivetrainProperties): PeriodicSignal[UnicycleSignal] = {
    import props._

    velocityControl(unicycle.map(s => UnicycleVelocity(
      maxForwardVelocity * s.forward, maxTurnVelocity * s.turn
    )))
  }
}
