package com.lynbrookrobotics.potassium.commons.drivetrain
import com.lynbrookrobotics.potassium.clock.Clock
import com.lynbrookrobotics.potassium.commons.cartesianPosition.XYPosition
import com.lynbrookrobotics.potassium.control.PIDF
import com.lynbrookrobotics.potassium.units.{GenericDerivative, Point, Ratio, Value3D}
import com.lynbrookrobotics.potassium.{Component, PeriodicSignal, Signal, SignalLike}
import org.scalatest.time.Milliseconds
import squants.Quantity
import squants.{Acceleration, Angle, Dimensionless, Length, Percent, Velocity}
import squants.motion._
import squants.space.{Degrees, Feet, Meters}
import squants.time.{Seconds, Time, TimeDerivative}

import scala.collection.mutable

/**
  * Simulate inputs and motor back emf (friction) to model kinematics of
  * a unicycleDrivetrain
  */
class SimulatedUnicycleHardware(
    maxAcceleration: Acceleration,
    maxTurnAcceleration: GenericDerivative[AngularVelocity],
    maxVelocity: Velocity,
    maxTurnVelocity: AngularVelocity,
    accelerationDueToFriction: Acceleration) extends UnicycleHardware {

  def this(maxAcceleration: Acceleration, maxVelocity: Velocity, track: Length, frictionDecceleration: Acceleration) {
    this(
      maxAcceleration,
      unitTools.linearToAngular(maxAcceleration, track),
      maxVelocity,
      unitTools.linearToAngular(maxVelocity, track),
      frictionDecceleration)
  }

  def this(properties: UnicycleProperties, track: Length, frictionDecceleration: Acceleration) {
    this(properties.maxAcceleration, properties.maxForwardVelocity, track, frictionDecceleration)
  }

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

  val periodicForwardPosition = periodicVelocity.scanLeft(Meters(0)) {
    case (acc, velocity, dt) => acc + velocity * dt
  }
  override val forwardPosition = peekLength(periodicForwardPosition)

  val periodicTurnPosition = periodicTurnVelocity.scanLeft(Degrees(0)) {
    case (acc, velocity, dt) => acc + velocity * dt
  }
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

class UnicyclePackageSimulator extends UnicycleDrive {
  override type Hardware = SimulatedUnicycleHardware
  override type Properties = UnicycleProperties

  override type DriveSignal = UnicycleSignal
  override type DriveVelocity = UnicycleSignal
  override protected def convertUnicycleToDrive(uni: UnicycleSignal): DriveSignal = uni


  override type Drivetrain = SimulatedUnicycleDriveComponent

  override protected def controlMode(implicit hardware: SimulatedUnicycleHardware, props: UnicycleProperties): UnicycleControlMode = ???

  /**
    * Drives with the signal with closed-loop control
    *
    * @param signal the signal to drive with
    * @return
    */
  override protected def driveClosedLoop(signal: SignalLike[UnicycleSignal])
                                        (implicit hardware: SimulatedUnicycleHardware,
                                        props: Signal[UnicycleProperties]): PeriodicSignal[UnicycleSignal] = {
    val targetForwardVelocity = signal.map(_.forward.toEach * props.get.maxForwardVelocity)
    val targetTurnVelocity = signal.map(_.turn.toEach * props.get.maxTurnVelocity)

    val forwardOutput = PIDF.pidf(
      hardware.forwardVelocity.toPeriodic,
      targetForwardVelocity.toPeriodic,
      props.map(_.forwardControlGainsFull))
    val turnOuput = PIDF.pid(
      hardware.turnVelocity.toPeriodic,
      targetTurnVelocity.toPeriodic,
      props.map(_.turnControlGains))

    forwardOutput.zip(turnOuput).map(o => UnicycleSignal(o._1, o._2))
  }
}

class SimulatedUnicycleDriveComponent(period: Time)
                                      (implicit hardware: SimulatedUnicycleHardware,
                                                clock: Clock) extends Component[UnicycleSignal](period) {
  override def defaultController: PeriodicSignal[UnicycleSignal] = Signal.constant(
    UnicycleSignal(Percent(0), Percent(0))
  ).toPeriodic

  /**
    * Applies the latest control signal value.
    *
    * @param signal the signal value to act on
    */
  override def applySignal(signal: UnicycleSignal): Unit = {
    hardware.acceptInput(signal, Seconds(0.005))
  }
}