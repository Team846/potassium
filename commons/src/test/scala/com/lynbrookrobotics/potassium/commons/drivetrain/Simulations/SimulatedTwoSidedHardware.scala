package com.lynbrookrobotics.potassium.commons.drivetrain.Simulations

import com.lynbrookrobotics.potassium.clock.Clock
import com.lynbrookrobotics.potassium.{PeriodicSignal, Signal}
import com.lynbrookrobotics.potassium.commons.cartesianPosition.XYPosition
import com.lynbrookrobotics.potassium.commons.drivetrain._
import com.lynbrookrobotics.potassium.units.{MomentOfInertia, NewtonMeters, Point, Torque}
import squants.{Acceleration, Angle, Dimensionless, Length, Mass, Percent, Velocity}
import squants.motion.{AngularVelocity, Force, _}
import squants.space.{Degrees, Meters}
import squants.time.{Seconds, Time}
import com.lynbrookrobotics.potassium.units.Conversions._

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
    wheelPositions.currentValue(dt)
//    leftPositionPeriodic.currentValue(dt)
//    rightPositionPeriodic.currentValue(dt)
    position.currentValue(dt)

    time += dt
    updateHistory()
  }

  val leftMotor = new SimulatedSpeedController
  val rightMotor = new SimulatedSpeedController

  implicit val MaxMotorForce = mass * props.maxAcceleration / 2
  private val leftForceOutput = leftMotor.outputSignal.map(o => o.toEach * MaxMotorForce)
  private val rightForceOutput = rightMotor.outputSignal.map(o => o.toEach * MaxMotorForce)

  def incrementVelocities(leftInputForce: Force,
                          rightInputForce: Force,
                          leftVelocity: Velocity,
                          rightVelocity: Velocity,
                          dt: Time): (Velocity, Velocity) = {
    val leftFriction = PhysicsUtil.friction(leftVelocity, constantFriction)
    val netLeftForce = leftInputForce + leftFriction

    val rightFriction = PhysicsUtil.friction(rightVelocity, constantFriction)
    val netRightForce = rightInputForce + rightFriction

    // radius from center, located halfway between wheels
    val radius = track / 2
    val torque: Torque = netRightForce * radius - netLeftForce * radius

    // Newton's second law
    val angularAcceleration = torque / momentOfInertia
    val linearAcceleration  = (netLeftForce + netRightForce) / mass

    // TODO: do this with implicit class conversions or define
    // TODO: AngularAcceleration class
    val newLeftVelocity = leftVelocity +
      (linearAcceleration - angularAcceleration.value * radius / (Seconds(1) * Seconds(1))) * dt
    val newRightVelocity = rightVelocity +
      (linearAcceleration + angularAcceleration.value * radius / (Seconds(1) * Seconds(1))) * dt

    (newLeftVelocity, newRightVelocity)
  }

  private val velocities = leftForceOutput.zip(rightForceOutput).scanLeft((MetersPerSecond(0), MetersPerSecond(0))) {
    case((leftVelocity, rightVelocity), (leftForceOut, rightForceOut), dt) =>
      incrementVelocities(leftForceOut, rightForceOut, leftVelocity, rightVelocity, dt)
  }


  private val leftSpeedPeriodic = velocities.map(_._1)
  private val rightSpeedPeriodic = velocities.map(_._2)

  override val leftVelocity: Signal[Velocity] = peekVelocity(leftSpeedPeriodic)
  override val rightVelocity: Signal[Velocity] = peekVelocity(rightSpeedPeriodic)

  val leftPositionPeriodic = leftSpeedPeriodic.integral
  val rightPositionPeriodic = rightSpeedPeriodic.integral

  override val leftPosition: Signal[Length] = peekLength(leftPositionPeriodic)
  override val rightPosition: Signal[Length] = peekLength(rightPositionPeriodic)

  val wheelPositions = leftPositionPeriodic.zip(rightPositionPeriodic)

  val position = XYPosition(turnPosition.map(a => Degrees(90) - a), forwardPosition)
  val peekedPosition: Signal[Point] = position.peek.map(_.getOrElse(Point.origin))

  def peekLength(toPeek: PeriodicSignal[Length]): Signal[Length] = {
    toPeek.peek.map(_.getOrElse(Meters(0)))
  }

  private def peekVelocity(toPeek: PeriodicSignal[Velocity]) = {
    toPeek.peek.map(_.getOrElse(MetersPerSecond(0)))
  }
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
  private def emfForce(velocity: Velocity)
                      (implicit props: UnicycleProperties,
                        maxMotorForce: Force): Force = {
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
  def friction(velocity: Velocity, constantFriction: Force)
              (implicit props: TwoSidedDriveProperties,
                        maxMotorForce: Force): Force = {
    val direction = -1 * Math.signum(velocity.value)

    direction * (emfForce(velocity) + constantFriction)
  }
}