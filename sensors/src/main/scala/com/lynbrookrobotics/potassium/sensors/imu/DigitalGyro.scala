package com.lynbrookrobotics.potassium.sensors.imu

import com.lynbrookrobotics.potassium.units.Value3D

import com.lynbrookrobotics.potassium.{PeriodicSignal, Signal}
import squants.motion.{AngularVelocity, DegreesPerSecond}
import squants.{Angle, Time}

import scala.collection.mutable

/**
  * Calibration, calculation for velocity
  * @param tickPeriod tick period of robot
  */
abstract class DigitalGyro(tickPeriod: Time) {
  // Tick Period of the robot
  var currentDrift: Value3D[AngularVelocity] = null

  // List of velocities used for calibration of IMU
  val calibrationVelocities: mutable.Queue[Value3D[AngularVelocity]] = mutable.Queue.empty

  // Current index in calibrationVelocities
  var index: Int = 0
  // Whether IMU is calibrating
  var calibrating: Boolean = true

  /**
    * Gets the current velocity
    * @return Value3D
    */
  def retrieveVelocity: Value3D[AngularVelocity]

  /**
    * Update values for the angle on the gyro.
    */
  def endCalibration(): Unit = {
    if (calibrating) {
      val sum = calibrationVelocities.reduceLeft { (acc, cur) =>
        acc + cur
      }

      currentDrift = sum.times(1D / calibrationVelocities.size)

      calibrating = false
    }
  }

  val velocity: PeriodicSignal[Value3D[AngularVelocity]] = Signal {
    if (calibrating) {
      calibrationVelocities.enqueue(retrieveVelocity)

      if (calibrationVelocities.size > 200) calibrationVelocities.dequeue()

      Value3D(DegreesPerSecond(0), DegreesPerSecond(0), DegreesPerSecond(0))
    } else {
      retrieveVelocity - currentDrift
    }
  }.toPeriodic

  val velocityX: PeriodicSignal[AngularVelocity] = velocity.map(_.x)
  val velocityY: PeriodicSignal[AngularVelocity] = velocity.map(_.y)
  val velocityZ: PeriodicSignal[AngularVelocity] = velocity.map(_.z)

  val positionX: PeriodicSignal[Angle] = velocityX.integral
  val positionY: PeriodicSignal[Angle] = velocityY.integral
  val positionZ: PeriodicSignal[Angle] = velocityZ.integral

  val position: PeriodicSignal[Value3D[Angle]] = {
    positionX.zip(positionY).zip(positionZ).map { t =>
      val ((x, y), z) = t
      Value3D(x, y, z)
    }
  }
}
