package com.lynbrookrobotics.potassium.commons.drivetrain.Simulations

import com.lynbrookrobotics.potassium.clock.Clock
import com.lynbrookrobotics.potassium.commons.cartesianPosition.XYPosition
import com.lynbrookrobotics.potassium.commons.drivetrain._
import com.lynbrookrobotics.potassium.control.PIDF
import com.lynbrookrobotics.potassium.units.{GenericDerivative, GenericValue, Point}
import com.lynbrookrobotics.potassium.{Component, PeriodicSignal, Signal, SignalLike}
import squants.motion._
import squants.space.{Degrees, Feet, Meters}
import squants.time.{Seconds, Time, TimeDerivative}
import squants.{Acceleration, Angle, Dimensionless, Length, Percent, Velocity}

import scala.collection.mutable

class SimulatedTwoSidedHardware(frictionAcc: Acceleration,
                                period: Time,
                                override val track: Length)
                                (implicit props: TwoSidedDriveProperties) extends TwoSidedDriveHardware {
  val history = new mutable.ArrayBuffer[(Time, Length, Angle, Velocity, AngularVelocity, Point)]
  var time = Seconds(0)

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

  def update(): Unit = {
    updateHistory()
    position.currentValue(period)
  }

  val leftMotor = new SimulatedSpeedController
  val rightMotor = new SimulatedSpeedController

  private def capAt100Percent(input: Dimensionless) = {
    input min Percent(100) max Percent(-100)
  }

  val leftAccelOutput = leftMotor.outputSignal.map(o => capAt100Percent(o).toEach * props.maxAcceleration / 2)
  val rightAccelOutput = rightMotor.outputSignal.map(o => capAt100Percent(o).toEach * props.maxAcceleration / 2)

  def incrementVelocity(velocity: Velocity, inputAcceleration: Acceleration, dt: Time): Velocity = {
    val frictionAcceleration = PhysicsUtil.friction(velocity, frictionAcc)

    // newton's second law, mass is unknown
    val acceleration = inputAcceleration + frictionAcceleration

    velocity + acceleration * dt
  }

  val leftSpeedPeriodic = leftAccelOutput.scanLeft(MetersPerSecond(0)) {
    case (velocity, inputAcceleration, dt) =>
      time += dt
      incrementVelocity(velocity, inputAcceleration, dt)
  }

  val rightSpeedPeriodic = rightAccelOutput.scanLeft(MetersPerSecond(0)) {
    case (velocity, inputAcceleration, dt) => incrementVelocity(velocity, inputAcceleration, dt)
  }

  val leftPositionPeriodic = leftSpeedPeriodic.integral
  val rightPositionPeriodic = rightSpeedPeriodic.integral

  val turnVelocityPeriodic = leftSpeedPeriodic.zip(rightSpeedPeriodic).map { s =>
    val (left, right) = s
    val diff = ((left - right) / 2).toMetersPerSecond
    RadiansPerSecond(diff / track.toMeters)
  }

  val turnPositionPeriodic = turnVelocityPeriodic.integral

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

  override val leftVelocity  = peekVelocity(leftSpeedPeriodic)
  override val rightVelocity = peekVelocity(rightSpeedPeriodic)

  val leftPosition = peekLength(leftPositionPeriodic)
  val rightPosition = peekLength(rightPositionPeriodic)

  val position = XYPosition(turnPosition.map(a => Degrees(90) - a), forwardPosition)
  val peekedPosition = position.peek.map(_.getOrElse(Point.origin))
}

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
  val position = XYPosition(turnPosition, forwardPosition)
  val peekedPosition = position.peek.map(_.getOrElse(new Point(Feet(0), Feet(0))))
}

object unitTools {
  def linearToAngular(speed: Velocity, radius: Length): AngularVelocity = {
    RadiansPerSecond(speed.toMetersPerSecond / radius.toMeters)
  }

  def linearToAngular(acceleration: Acceleration, radius: Length) = {
    new GenericDerivative(
      acceleration.toMetersPerSecondSquared / radius.toMeters,
      RadiansPerSecond
    )
  }
}

class TwoSidedDrivePackageSimulator(period: Time)(implicit clock: Clock) extends TwoSidedDrive(period) {
  override type Hardware = SimulatedTwoSidedHardware
  override type Properties = TwoSidedDriveProperties

 /**
    * Output the current signal to actuators with the hardware
    *
    * @param hardware the hardware to output with
    * @param signal   the signal to output
    */
  override protected def output(hardware: Hardware, signal: TwoSidedSignal): Unit = {
    hardware.leftMotor.set(signal.left)
    hardware.rightMotor.set(signal.right)
    hardware.update()
  }

  override protected def controlMode(implicit hardware: SimulatedTwoSidedHardware,
    props: Properties): UnicycleControlMode = ???
}

object PhysicsUtil {
  /**
    * motor back emf is proportional to speed
    * @param velocity
    * @param props
    * @return
    */
  private def emfAcceleration(velocity: Velocity)(implicit props: UnicycleProperties): Acceleration = {
    (velocity / props.maxForwardVelocity) * props.maxAcceleration
  }

  // friction accounting constant friction and motor back emf
  def friction(velocity: Velocity, constantFriction: Acceleration)(implicit props: TwoSidedDriveProperties): Acceleration = {
    val direction = -1 * Math.signum(velocity.value)

    direction * (emfAcceleration(velocity) + constantFriction)
  }
}

object Calculus {
//  def eulersMethod[T <: GenericDerivative[T]](initialValue: GenericValue[T],
//                      derivative: T => GenericDerivative[T]): GenericValue[T] = {
//    Signal(initialValue).toPeriodic.scanLeft(initialValue) {
//      case (acc, curr, dt ) =>
//        acc + dt * derivative(curr)
//    }
//  }
}