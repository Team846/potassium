package com.lynbrookrobotics.potassium.model.simulations

import com.lynbrookrobotics.potassium.clock.Clock
import com.lynbrookrobotics.potassium.{PeriodicSignal, Signal}
import com.lynbrookrobotics.potassium.commons.cartesianPosition.XYPosition
import com.lynbrookrobotics.potassium.commons.drivetrain._
import com.lynbrookrobotics.potassium.units.Point
import squants.mass.MomentOfInertia
import squants.{Acceleration, Angle, Dimensionless, Length, Mass, Percent, Velocity}
import squants.motion.{AngularVelocity, Force, _}
import squants.space.{Degrees, Meters, Radians}
import squants.time.{Seconds, Time}

import scala.collection.mutable

case class MomentInHistory(time: Time,
                           forwardPosition: Length,
                            angle: Angle,
                            forwardVelocity: Velocity,
                            turnSpeed: AngularVelocity,
                            position: Point)

class SimulatedTwoSidedHardware(constantFriction: Force,
                                override val track: Length,
                                mass: Mass,
                                momentOfInertia: MomentOfInertia)
                                (implicit props: TwoSidedDriveProperties) extends TwoSidedDriveHardware {
  val history = new mutable.ArrayBuffer[MomentInHistory]
  private var time = Seconds(0)

  def updateHistory() {
    history.append(
      MomentInHistory(
        time,
        forwardPosition.get,
        turnPosition.get,
        forwardVelocity.get,
        turnVelocity.get,
        peekedPosition.get
      )
    )
  }

  def update(dt: Time): Unit = {
    // force periodic signals to update
    DrivetrainPositions.currentValue(dt)
    position.currentValue(dt)

    time += dt
    updateHistory()
  }

  val leftMotor = new SimulatedSpeedController
  val rightMotor = new SimulatedSpeedController

  private val maxMotorForce = mass * props.maxAcceleration / 2
  private val leftForceOutput = leftMotor.outputSignal.map(o => o.toEach * maxMotorForce)
  private val rightForceOutput = rightMotor.outputSignal.map(o => o.toEach * maxMotorForce)

  def incrementVelocities(leftInputForce: Force,
                          rightInputForce: Force,
                          leftVelocity: Velocity,
                          rightVelocity: Velocity,
                          angularVelocity: AngularVelocity,
                          dt: Time): (Velocity, Velocity, AngularVelocity) = {
    val leftFriction = PhysicsUtil.friction(leftVelocity, constantFriction, maxMotorForce)
    val netLeftForce = leftInputForce + leftFriction
    val rightFriction = PhysicsUtil.friction(rightVelocity, constantFriction, maxMotorForce)
    val netRightForce = rightInputForce + rightFriction

    // radius from center, located halfway between wheels
    val radius = track / 2
    val netTorque = (netRightForce * radius - netLeftForce * radius).asTorque

    // Newton's second laws
    val angularAcceleration = netTorque / momentOfInertia
    val linearAcceleration  = (netLeftForce + netRightForce) / mass

    // Linear acceleration caused by angular acceleration about the center
    val tangentialAcceleration = angularAcceleration onRadius radius

    // Euler's method to integrate velocities
    val newLeftVelocity = leftVelocity + (linearAcceleration - tangentialAcceleration) * dt
    val newRightVelocity = rightVelocity + (linearAcceleration + tangentialAcceleration) * dt
    val newAngularVelocity = angularVelocity + angularAcceleration * dt

    (newLeftVelocity, newRightVelocity, newAngularVelocity)
  }

  private val InitialSpeeds = (MetersPerSecond(0), MetersPerSecond(0), RadiansPerSecond(0))
  private val velocities = leftForceOutput.zip(rightForceOutput).scanLeft(InitialSpeeds) {
    case ((leftVelocity, rightVelocity, turnVelocity), (leftForceOut, rightForceOut), dt) =>
      incrementVelocities(leftForceOut, rightForceOut, leftVelocity, rightVelocity, turnVelocity, dt)
  }

  private val leftSpeedPeriodic: PeriodicSignal[Velocity] = velocities.map(_._1)
  private val rightSpeedPeriodic: PeriodicSignal[Velocity] = velocities.map(_._2)

  // convert triginometric velocity to compass velocity
  private val turnVelocityPeriodic: PeriodicSignal[AngularVelocity] = velocities.map(-1 * _._3)

  private val leftPositionPeriodic: PeriodicSignal[Length] = leftSpeedPeriodic.integral
  private val rightPositionPeriodic: PeriodicSignal[Length] = rightSpeedPeriodic.integral
  private val turnPositionPeriodic: PeriodicSignal[Angle] = turnVelocityPeriodic.integral

  // zip all position signals together so that when this value is updated,
  // all positions are updated only once
  private val DrivetrainPositions = leftPositionPeriodic.zip(rightPositionPeriodic).zip(turnPositionPeriodic)

  private def peekLength(toPeek: PeriodicSignal[Length]): Signal[Length] = {
    toPeek.peek.map(_.getOrElse(Meters(0)))
  }

  private def peekVelocity(toPeek: PeriodicSignal[Velocity]) = {
    toPeek.peek.map(_.getOrElse(MetersPerSecond(0)))
  }

  private def peekAngularVelocity(toPeek: PeriodicSignal[AngularVelocity]) = {
    toPeek.peek.map(_.getOrElse(RadiansPerSecond(0)))
  }

  private def peekAngle(toPeek: PeriodicSignal[Angle]) = {
    toPeek.peek.map(_.getOrElse(Radians(0)))
  }

  val position = XYPosition(turnPosition.map(a => Degrees(90) - a), forwardPosition)

  override val leftVelocity: Signal[Velocity]             = peekVelocity(leftSpeedPeriodic)
  override val rightVelocity: Signal[Velocity]            = peekVelocity(rightSpeedPeriodic)
  val trigSpeed = peekAngularVelocity(turnVelocityPeriodic)
  override lazy val turnVelocity: Signal[AngularVelocity] = peekAngularVelocity(turnVelocityPeriodic)

  // I have absolutely no idea why, but marking these as lazy fixes null
  // pointer exceptions
  override lazy val leftPosition: Signal[Length]  = peekLength(leftPositionPeriodic)
  override lazy val rightPosition: Signal[Length] = peekLength(rightPositionPeriodic)

  override lazy val turnPosition: Signal[Angle]   = peekAngle(turnPositionPeriodic)
  private val peekedPosition: Signal[Point]       = position.peek.map(_.getOrElse(Point.origin))
}

class TwoSidedDriveContainerSimulator(period: Time)(implicit clock: Clock) extends TwoSidedDrive(period) {
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
    hardware.update(period)
  }

  override protected def controlMode(implicit hardware: SimulatedTwoSidedHardware,
                                              props: TwoSidedDriveProperties): UnicycleControlMode = {
    NoOperation
  }
}

object PhysicsUtil {
  /**
    * motor back emf is proportional to speed
    * @param velocity
    * @param props
    * @return
    */
  private def emfForce(velocity: Velocity, maxMotorForce: Force)
                      (implicit props: UnicycleProperties): Force = {
    val normalizedVelocity = velocity / props.maxForwardVelocity
    normalizedVelocity * maxMotorForce
  }

  /**
    * friction accounting constant friction and motor back emf
    * @param velocity
    * @param constantFriction
    * @param props
    * @return
    */
  def friction(velocity: Velocity, constantFriction: Force, maxMotorForce: Force)
              (implicit props: TwoSidedDriveProperties): Force = {
    val direction = -1 * Math.signum(velocity.value)

    direction * (emfForce(velocity, maxMotorForce) + constantFriction)
  }
}