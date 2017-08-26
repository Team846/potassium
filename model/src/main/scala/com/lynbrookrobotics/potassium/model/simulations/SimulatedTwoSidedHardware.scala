package com.lynbrookrobotics.potassium.model.simulations

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

// TODO: Already implemented in TwoSidedDrive
case class TwoSidedDriveForce(left: Force, right: Force)

class SimulatedTwoSidedHardware(constantFriction: Force,
                                override val track: Length,
                                mass: Mass,
                                momentOfInertia: MomentOfInertia,
                                clock: Clock,
                                period: Time)
                                (implicit props: TwoSidedDriveProperties) extends TwoSidedDriveHardware {
  val history = new mutable.ArrayBuffer[MomentInHistory]

  // TODO: breaks without a non null period
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

    // radius from center, located halfway between wheels
    val radius = track / 2
    val netTorque = (netRightForce * radius - netLeftForce * radius).asTorque

    // Newton's second laws
    val angularAcceleration = netTorque / momentOfInertia
    val linearAcceleration  = (netLeftForce + netRightForce) / mass

    // Linear acceleration caused by angular acceleration about the center
    val tangentialAcceleration = angularAcceleration onRadius radius

    // TODO: This is very suspicious and most likely wrong
    // Euler's method to integrate velocities
    val newLeftVelocity = leftVelocity + (linearAcceleration - tangentialAcceleration) * dt
    val newRightVelocity = rightVelocity + (linearAcceleration + tangentialAcceleration) * dt
    val newAngularVelocity = angularVelocity + angularAcceleration * dt

    RobotVelocities(newLeftVelocity, newRightVelocity, newAngularVelocity)
  }

  private val InitialSpeeds = RobotVelocities(MetersPerSecond(0), MetersPerSecond(0), RadiansPerSecond(0))

  private val twoSidedOutputs = leftForceOutput.zip(rightForceOutput).map(o =>
    TwoSidedDriveForce(o._1, o._2))

  twoSidedOutputs.foreach(_ => {
    if(debugMode) {
      println("two sided published")
    }
  })

  private val velocities = twoSidedOutputs.scanLeftWithdt(InitialSpeeds) {
    case (accVelocities, outputs, dt) => incrementVelocities(
      outputs.left,
      outputs.right,
      accVelocities.left,
      accVelocities.right,
      accVelocities.angular,
      dt)
  }

  override val leftVelocity = velocities.map(_.left)
  override val rightVelocity = velocities.map(_.right)

  leftVelocity.foreach{_ => {
    if(debugMode) {
      println("left velocity updated")
    }
  }}

  rightVelocity.foreach{_ => {
    if(debugMode) {
      println("right velocity updated")
    }
  }}

  forwardVelocity.foreach{_ => {
    if(debugMode) {
      println("forward velocity updated")
    }
  }}

  // convert triginometric velocity to compass velocity
  override lazy val turnVelocity = velocities.map(-1 * _.angular)

  override val leftPosition = leftVelocity.integral
  override val rightPosition = rightVelocity.integral

  leftPosition.foreach(_ => {
    if(debugMode) {
      println("left position updated")
    }
  })

  rightPosition.foreach(_ => {
    if(debugMode) {
      println("right position updated")
    }
  })


  override lazy val turnPosition = turnVelocity.integral

  val position = XYPosition(turnPosition.map(a => Degrees(90) - a), forwardPosition)

  val zippedPositions = forwardPosition.zip(turnPosition).zip(position)

  var debugMode = false

  forwardPosition.foreach(_ => {
    if (debugMode) {
      println("forward position updated")
    }
  })

  turnPosition.foreach(_ => {
    if (debugMode) {
      println("turn position updated")
    }
  })

  position.foreach(_ => {
    if (debugMode) {
      println("2d position updated")
    }
  })



  zippedPositions.foreach(_ => {
    if (debugMode) {
      println("zipped positions updated")
    }
  })
  val historyStream = zippedPositions.zip(forwardVelocity).zip(turnVelocity).zipWithTime.map{
    case (((((fPos, tPos), pos), fVel), tVel), time) =>
      MomentInHistory(
        time = time,
        forwardPosition = fPos,
        forwardVelocity = fVel,
        angle = tPos,
        position = pos,
        turnSpeed = tVel)
  }

  historyStream.foreach {
    history.append(_)
  }
}

class TwoSidedDriveContainerSimulator(period: Time)(val clock: Clock) extends TwoSidedDrive(period)(clock) {
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
  def frictionAndEMF(velocity: Velocity, constantFriction: Force, maxMotorForce: Force)
                    (implicit props: TwoSidedDriveProperties): Force = {
    val direction = -1 * Math.signum(velocity.value)

    direction * (emfForce(velocity, maxMotorForce) + constantFriction)
  }
}