package com.lynbrookrobotics.potassium.model.simulations

import com.lynbrookrobotics.potassium.{Component, Signal}
import com.lynbrookrobotics.potassium.clock.Clock
import com.lynbrookrobotics.potassium.commons.cartesianPosition.XYPosition
import com.lynbrookrobotics.potassium.commons.drivetrain._
import com.lynbrookrobotics.potassium.commons.drivetrain.TwoSidedDrive
import com.lynbrookrobotics.potassium.streams.Stream
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

case class RobotVelocities(left: Velocity,
                           right: Velocity,
                           angular: AngularVelocity)

case class TwoSidedDriveForce(left: Force, right: Force)

class SimulatedTwoSidedHardware(constantFriction: Force,
                                override val track: Length,
                                mass: Mass,
                                momentOfInertia: MomentOfInertia,
                                clock: Clock,
                                period: Time)
                                (implicit props: TwoSidedDriveProperties) extends TwoSidedDriveHardware {
  val leftMotor = new SimulatedMotor(clock, period)
  val rightMotor = new SimulatedMotor(clock, period)

  private val maxMotorForce = mass * props.maxAcceleration / 2
  private val leftForceOutput = leftMotor.outputStream.map(_.toEach * maxMotorForce)
  private val rightForceOutput = rightMotor.outputStream.map(_.toEach * maxMotorForce)

  def incrementVelocities(leftInputForce: Force,
                          rightInputForce: Force,
                          leftVelocity: Velocity,
                          rightVelocity: Velocity,
                          angularVelocity: AngularVelocity,
                          dt: Time): RobotVelocities = {
    val leftFriction = PhysicsUtil.frictionAndEMF(leftVelocity, constantFriction, maxMotorForce)
    val netLeftForce = leftInputForce + leftFriction
    val rightFriction = PhysicsUtil.frictionAndEMF(rightVelocity, constantFriction, maxMotorForce)
    val netRightForce = rightInputForce + rightFriction

    // Newton's second law for linear acceleration
    val leftAccel = netLeftForce / mass
    val rightAccel = netRightForce / mass

    // Euler's method to integrate
    val newLeftVelocity = leftVelocity + leftAccel * dt
    val newRightVelocity = rightVelocity + rightAccel  * dt

    // radius from center, located halfway between wheels
    val radius = track / 2
    val netTorque = (netRightForce * radius - netLeftForce * radius).asTorque

    // Newton's second law for angular acceleration
    val angularAcceleration = netTorque / momentOfInertia

    val newAngularVelocity = angularVelocity + angularAcceleration * dt

    RobotVelocities(newLeftVelocity, newRightVelocity, newAngularVelocity)
  }

  private val InitialSpeeds = RobotVelocities(MetersPerSecond(0), MetersPerSecond(0), RadiansPerSecond(0))

  private val twoSidedOutputs = leftForceOutput.zip(rightForceOutput).map(o =>
    TwoSidedDriveForce(o._1, o._2))

  private val velocities = twoSidedOutputs.zipWithDt.scanLeft(InitialSpeeds) {
    case (accVelocities, (outputs, dt)) =>
      incrementVelocities(
        outputs.left,
        outputs.right,
        accVelocities.left,
        accVelocities.right,
        accVelocities.angular,
        dt)
  }

  def listenTo[T](stream: Stream[T]): () => Option[T] = {
    var previous: Option[T] = None
    val handle = stream.foreach(v => previous = Some(v))
    () => {
      // hold reference to handle to prevent garbage collection
      handle.hashCode()
      previous
    }
  }

  override val leftVelocity = velocities.map(_.left)
  override val rightVelocity = velocities.map(_.right)

  // convert triginometric velocity to compass velocity
  override lazy val turnVelocity = velocities.map(-1 * _.angular)

  override val leftPosition = leftVelocity.integral
  override val rightPosition = rightVelocity.integral

  override lazy val turnPosition = turnVelocity.integral

  val position = XYPosition(turnPosition.map(a => Degrees(90) - a), forwardPosition)

  val zippedPositions = forwardPosition.zip(turnPosition).zip(position)

  val historyStream = zippedPositions.zip(forwardVelocity).zip(turnVelocity).zipWithTime.map {
    case (((((fPos, tPos), pos), fVel), tVel), time) =>
      MomentInHistory(
        time = time,
        forwardPosition = fPos,
        forwardVelocity = fVel,
        angle = tPos,
        position = pos,
        turnSpeed = tVel)
  }

  val positionListening = listenTo(position)
  val velocityListening = listenTo(forwardVelocity)
  val angleListening = listenTo(turnPosition)
}

class TwoSidedDriveContainerSimulator extends TwoSidedDrive { self =>
  override type Hardware = SimulatedTwoSidedHardware
  override type Properties = TwoSidedDriveProperties

  /**
    * Output the current signal to actuators with the hardware. In this case
    * it simply updates the value that simulated motors will publish the next
    * time that update() in an instance of SimulatedTwoSidedHardware is called
    *
    * @param hardware the simulated hardware to output with
    * @param signal   the signal to output
    */
  override protected def output(hardware: Hardware, signal: TwoSidedSignal): Unit = {
    hardware.leftMotor.set(signal.left)
    hardware.rightMotor.set(signal.right)
  }

  override protected def controlMode(implicit hardware: SimulatedTwoSidedHardware,
                                              props: TwoSidedDriveProperties): UnicycleControlMode = {
    NoOperation
  }

  class Drivetrain(implicit hardware: Hardware,
                   props: Signal[Properties]) extends Component[TwoSidedSignal] {
    override def defaultController: Stream[TwoSidedSignal] = self.defaultController

    override def applySignal(signal: TwoSidedSignal): Unit = {
      output(hardware, signal)
    }
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
    -1 * normalizedVelocity * maxMotorForce
  }

  /**
    * friction accounting constant friction and motor back emf
    * @param velocity
    * @param constantFriction
    * @param props
    * @return
    */
  def frictionAndEMF(velocity: Velocity, constantFriction: Force, maxMotorForce: Force)
                    (implicit props: TwoSidedDriveProperties): Force = {
    val direction = -1 * Math.signum(velocity.value)

    emfForce(velocity, maxMotorForce) + direction * constantFriction
  }
}