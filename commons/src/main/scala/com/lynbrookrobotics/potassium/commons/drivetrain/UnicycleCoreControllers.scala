package com.lynbrookrobotics.potassium.commons.drivetrain

import com.lynbrookrobotics.potassium.Signal
import com.lynbrookrobotics.potassium.streams.Stream
import com.lynbrookrobotics.potassium.control.PIDF
import squants.{Angle, Dimensionless, Length, Percent, Quantity, Time}
import com.lynbrookrobotics.potassium.units._

import scala.collection.immutable.Queue

trait UnicycleCoreControllers {
  type DriveSignal
  type DrivetrainHardware <: UnicycleHardware
  type DrivetrainProperties <: UnicycleProperties

  def lowerLevelOpenLoop(unicycle: Stream[UnicycleSignal]): Stream[DriveSignal] = parentOpenLoop(unicycle)

  def parentOpenLoop(unicycle: Stream[UnicycleSignal]): Stream[DriveSignal]

  def lowerLevelVelocityControl(unicycle: Stream[UnicycleSignal])(implicit hardware: DrivetrainHardware,
                                                                      props: Signal[DrivetrainProperties]): Stream[DriveSignal]

  def openForwardOpenDrive(forwardSpeed: Stream[Dimensionless]): Stream[DriveSignal] = {
    parentOpenLoop(forwardSpeed.map(f => UnicycleSignal(f, Percent(0))))
  }

  def openForwardClosedDrive(forwardSpeed: Stream[Dimensionless])(implicit hardware: DrivetrainHardware,
                                                                  props: Signal[DrivetrainProperties]): Stream[DriveSignal] = {
    lowerLevelVelocityControl(forwardSpeed.map(f => UnicycleSignal(f, Percent(0))))
  }

  def openTurnOpenDrive(turnSpeed: Stream[Dimensionless]): Stream[DriveSignal] = {
    parentOpenLoop(turnSpeed.map(t => UnicycleSignal(Percent(0), t)))
  }

  def openTurnClosedDrive(turnSpeed: Stream[Dimensionless])(implicit hardware: DrivetrainHardware,
                                                            props: Signal[DrivetrainProperties]): Stream[DriveSignal] = {
    lowerLevelVelocityControl(turnSpeed.map(t => UnicycleSignal(Percent(0), t)))
  }

  def velocityControl(target: Stream[UnicycleVelocity])
                     (implicit hardware: DrivetrainHardware,
                      props: Signal[DrivetrainProperties]): Stream[UnicycleSignal] = {
    import hardware._

    val forwardControl = PIDF.pidf(
      forwardVelocity,
      target.map(_.forward),
      props.map(_.forwardControlGainsFull)
    )

    val turnControl = PIDF.pidf(
      turnVelocity,
      target.map(_.turn),
      props.map(_.turnControlGainsFull)
    )

    forwardControl.zip(turnControl).map(s => UnicycleSignal(s._1, s._2))
  }

  def speedControl(unicycle: Stream[UnicycleSignal])
                       (implicit hardware: DrivetrainHardware,
                        props: Signal[DrivetrainProperties]): Stream[UnicycleSignal] = {
    velocityControl(unicycle.map(s => UnicycleVelocity(
      props.get.maxForwardVelocity * s.forward, props.get.maxTurnVelocity * s.turn
    )))
  }

  def calculateTargetFromOffsetWithLatency[T <: Quantity[T]]
    (timestampedOffset: Stream[(T, Time)],
     positionSlide: Stream[Queue[(T, Time)]]) = {
    positionSlide.zip(timestampedOffset).map { t =>
      val (positionHistory, (offset, offsetTime)) = t
        val closestTimeSoFar = positionHistory.minBy{ case (position, positionTime) =>
          Math.abs(positionTime.value - offsetTime.value)
        }

        closestTimeSoFar._1 + offset
    }
  }

  def forwardPositionControl(targetAbsolute: Length)
                            (implicit hardware: DrivetrainHardware,
                             props: Signal[DrivetrainProperties]): (Stream[UnicycleSignal], Stream[Length]) = {
    val error = hardware.forwardPosition.map(targetAbsolute - _)

    val control = PIDF.pid(
      hardware.forwardPosition,
      hardware.forwardPosition.mapToConstant(targetAbsolute),
      props.map(_.forwardPositionControlGains)
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
      props.map(_.turnPositionControlGains)
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
      props.map(_.turnPositionControlGains)
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
      props.map(_.turnPositionControlGains)
    ).map(s => UnicycleSignal(Percent(0), s))

    (control, error)
  }
}
