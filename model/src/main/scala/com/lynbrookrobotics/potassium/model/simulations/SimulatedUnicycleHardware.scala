package com.lynbrookrobotics.potassium.model.simulations

import com.lynbrookrobotics.potassium.clock.Clock
import com.lynbrookrobotics.potassium.commons.cartesianPosition.XYPosition
import com.lynbrookrobotics.potassium.commons.drivetrain._
import com.lynbrookrobotics.potassium.control.PIDF
import com.lynbrookrobotics.potassium.units.{GenericDerivative, GenericValue, Point}
import com.lynbrookrobotics.potassium.{Component, PeriodicSignal, Signal, SignalLike}
import squants.motion._
import squants.space.{Degrees, Feet, Meters}
import squants.time.{Milliseconds, Seconds, Time, TimeDerivative}
import squants.{Acceleration, Angle, Dimensionless, Length, Percent, Velocity}

import scala.collection.mutable



/**
  * Simulate inputs and motor back emf (friction) to model kinematics of
  * a unicycleDrivetrain
  */
class SimulatedUnicycleHardware(
    props: UnicycleProperties,
    maxTurnAcceleration: GenericDerivative[AngularVelocity],
    accelerationDueToFriction: Acceleration) extends UnicycleHardware {

  val maxAcceleration: Acceleration =  props.maxAcceleration
  val maxVelocity: Velocity = props.maxForwardVelocity
  val maxTurnVelocity: AngularVelocity = props.maxTurnVelocity

  private def capAt100Percent(input: Dimensionless) = {
    input min Percent(100) max Percent(-100)
  }

  val history = new mutable.ArrayBuffer[(Time, Length, Angle, Velocity, AngularVelocity, Point)]
  var time = Seconds(0)

  def clearHistory() {
    history.clear()
    time = Seconds(0)
  }

  def updateHistory() {
    history.append(
      (
        time,
        forwardPosition.get,
        turnPosition.get,
        forwardVelocity.get,
        turnVelocity.get,
        peekedPosition.get
      ))
  }
  var lastOutput: UnicycleSignal = _

  def updateData(dt: Time): Unit = {
    periodicForwardPosition.currentValue(dt)
    periodicTurnPosition.currentValue(dt)
    position.currentValue(dt)
  }

  /**
    * component's applySignal method should call this method
    * @param unicycleSignal
    */
  def acceptInput(unicycleSignal: UnicycleSignal, period: Time) {
    lastOutput = unicycleSignal
    updateHistory()
    updateData(period)
  }

  val inputForwardAcceleration = Signal(capAt100Percent(lastOutput.forward).toEach * maxAcceleration).toPeriodic

  val periodicVelocity = inputForwardAcceleration.scanLeft(FeetPerSecond(0)){
    case (velocity, inputAcceleration, dt) =>
      time += dt
      val directionFriction = -1 * Math.signum(velocity.value)

      // motor back emf is proportional to speed
      val emfDecceleration = maxAcceleration * (velocity / maxVelocity)
      val frictionAcceleration = directionFriction * (emfDecceleration + accelerationDueToFriction)

      // newton's second law, mass is unknown
      val acceleration = inputAcceleration + frictionAcceleration

      velocity + acceleration * dt
  }

  override val forwardVelocity = peekVelocity(periodicVelocity)

  var lastTurnAcceleration = Signal(maxTurnAcceleration * capAt100Percent(lastOutput.turn).toEach)

  val periodicTurnVelocity = lastTurnAcceleration.toPeriodic.scanLeft(DegreesPerSecond(0)){
    case (velocity, inputAcceleration, dt) =>
      val directionFriction = -1 * Math.signum(velocity.value)

      val emfDecceleration = maxTurnAcceleration * (velocity.abs / maxTurnVelocity)
      val frictionAcceleration = directionFriction * (emfDecceleration)
      // TODO: include turning friction similar to what is in forward motion

      val acceleration = inputAcceleration + emfDecceleration

      velocity + acceleration * dt
  }

  override val turnVelocity = peekTurnVelocity(periodicTurnVelocity)

  val periodicForwardPosition = periodicVelocity.integral
  override val forwardPosition = peekLength(periodicForwardPosition)

  val periodicTurnPosition = periodicTurnVelocity.integral
  override val turnPosition = peekAngle(periodicTurnPosition)

  val position = XYPosition(turnPosition, forwardPosition)
  val peekedPosition = position.peek.map(_.getOrElse(new Point(Feet(0), Feet(0))))

  def peekAngle(toPeek: PeriodicSignal[Angle]): Signal[Angle] = {
    toPeek.peek.map(_.getOrElse(Degrees(0)))
  }

  def peekLength(toPeek: PeriodicSignal[Length]): Signal[Length] = {
    toPeek.peek.map(_.getOrElse(Meters(0)))
  }

  def peekTurnVelocity(toPeek: PeriodicSignal[AngularVelocity]) = {
    toPeek.peek.map(_.getOrElse(DegreesPerSecond(0)))
  }

  def peekVelocity(toPeek: PeriodicSignal[Velocity]) = {
    toPeek.peek.map(_.getOrElse(MetersPerSecond(0)))
  }
}