package com.lynbrookrobotics.potassium.commons.drivetrain.unicycle.control

import com.lynbrookrobotics.potassium.Signal
import com.lynbrookrobotics.potassium.commons.drivetrain.unicycle.{UnicycleHardware, UnicycleProperties, UnicycleSignal, UnicycleVelocity}
import com.lynbrookrobotics.potassium.control.PIDF
import com.lynbrookrobotics.potassium.streams.Stream
import squants.{Angle, Length, Percent}

trait UnicycleCoreControllers {
  type DriveSignal
  type OpenLoopSignal
  type DrivetrainHardware <: UnicycleHardware
  type DrivetrainProperties <: UnicycleProperties

  def openLoopToDriveSignal(openLoop: OpenLoopSignal): DriveSignal

  def childOpenLoop(unicycle: Stream[UnicycleSignal]): Stream[OpenLoopSignal]

  def childVelocityControl(unicycle: Stream[UnicycleSignal])
                          (implicit hardware: DrivetrainHardware,
                           props: Signal[DrivetrainProperties]): Stream[DriveSignal]

  def velocityControl(target: Stream[UnicycleVelocity])
                     (implicit hardware: DrivetrainHardware,
                      props: Signal[DrivetrainProperties]): Stream[UnicycleSignal] = {
    import hardware._

    val forwardControl = PIDF.pidf(
      forwardVelocity,
      target.map(_.forward),
      props.map(_.forwardVelocityGainsFull)
    )

    val turnControl = PIDF.pidf(
      turnVelocity,
      target.map(_.turn),
      props.map(_.turnVelocityGainsFull)
    )

    forwardControl.zip(turnControl).map { s =>
      UnicycleSignal(s._1, s._2)
    }
  }

  def speedControl(unicycle: Stream[UnicycleSignal])
                       (implicit hardware: DrivetrainHardware,
                        props: Signal[DrivetrainProperties]): Stream[UnicycleSignal] =
    velocityControl(unicycle.map(_.toUnicycleVelocity))

  def forwardPositionControl(targetAbsolute: Length)
                            (implicit hardware: DrivetrainHardware,
                             props: Signal[DrivetrainProperties]): (Stream[UnicycleSignal], Stream[Length]) = {
    val error = hardware.forwardPosition.map(targetAbsolute - _)

    val control = PIDF.pid(
      hardware.forwardPosition,
      hardware.forwardPosition.mapToConstant(targetAbsolute),
      props.map(_.forwardPositionGains)
    ).map(s => UnicycleSignal(s, Percent(0)))

    (control, error)
  }

  def forwardPositionControl(targetAbsolute: Stream[Length])
                            (implicit hardware: DrivetrainHardware,
                             props: Signal[DrivetrainProperties]): (Stream[UnicycleSignal], Stream[Length]) = {
    val error: Stream[Length] = targetAbsolute.minus(hardware.forwardPosition)

    val control = PIDF.pid(
      hardware.forwardPosition,
      targetAbsolute,
      props.map(_.forwardPositionGains)
    ).map(s => UnicycleSignal(s, Percent(0)))

    (control, error)
  }

  def continuousTurnPositionControl(targetAbsolute: Stream[Angle])
    (implicit hardware: DrivetrainHardware,
      props: Signal[DrivetrainProperties]): (Stream[UnicycleSignal], Stream[Angle]) = {
    val error = targetAbsolute.zip(hardware.turnPosition).map{ t =>
      val (target: Angle, pos: Angle) = t
      target - pos
    }

    val control = PIDF.pid(
      hardware.turnPosition,
      targetAbsolute,
      props.map(_.turnPositionGains)
    ).map(s => UnicycleSignal(Percent(0), s))

    (control, error)
  }

  def turnPositionControl(targetAbsolute: Angle)
    (implicit hardware: DrivetrainHardware,
      props: Signal[DrivetrainProperties]): (Stream[UnicycleSignal], Stream[Angle]) = {
    val error = hardware.turnPosition.map(targetAbsolute - _)

    val control = PIDF.pid(
      hardware.turnPosition,
      hardware.turnPosition.mapToConstant(targetAbsolute),
      props.map(_.turnPositionGains)
    ).map(s => UnicycleSignal(Percent(0), s))

    (control, error)
  }

  def turnPositionControl(targetAbsolute: Stream[Angle])
    (implicit hardware: DrivetrainHardware,
      props: Signal[DrivetrainProperties]): (Stream[UnicycleSignal], Stream[Angle]) = {
    val error = hardware.turnPosition.zip(targetAbsolute).map(a => a._1 - a._2)

    val control = PIDF.pid(
      hardware.turnPosition,
      targetAbsolute,
      props.map(_.turnPositionGains)
    ).map(s => UnicycleSignal(Percent(0), s))

    (control, error)
  }
}
