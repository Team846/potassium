package com.lynbrookrobotics.potassium.sensors.imu

import java.util
import com.lynbrookrobotics.potassium.{PeriodicSignal, Signal}
import squants.motion.{AngularVelocity, DegreesPerSecond}
import squants.{Angle, Time}

/**
  * Calibration, calculation for velocity
  * @param tickPeriod tick period of robot
  */
abstract class DigitalGyro(tickPeriod: Time) {
  // Tick Period of the robot
  var currentVelocity: Value3D[AngularVelocity] = Value3D[AngularVelocity](DegreesPerSecond(0), DegreesPerSecond(0), DegreesPerSecond(0))
  var currentDrift: Value3D[AngularVelocity] = null

  // List of velocities used for calibration of IMU
  var calibrationVelocities: util.ArrayList[Value3D[AngularVelocity]] = new util.ArrayList(200)

  // Current index in calibrationVelocities
  var index: Int = 0

  // Whether IMU is calibrating
  var calibrating: Boolean = true

  // Gets the current velocity.
  def retrieveVelocity: Value3D[AngularVelocity]

  // Update velocity stored calibrationVelocities at index
  def calibrateUpdate(): Unit = {
    currentVelocity = retrieveVelocity
    calibrationVelocities.set(index, currentVelocity)
    index += 1

    if (index >= 200) index = 0
  }

  // Updates values for the angle on the gyro.
  def angleUpdate(): Unit = {
    if (calibrating) {
      val sum = Value3D[AngularVelocity](DegreesPerSecond(0), DegreesPerSecond(0), DegreesPerSecond(0))
      calibrationVelocities.forEach(
        v => sum + v
      )

      currentDrift = sum.times(-1D / calibrationVelocities.size())
      calibrationVelocities = null
      calibrating = false
    }

    // Stores the value as a form of memory
    // Modifies velocity according to drift and change in position
    currentVelocity = retrieveVelocity + currentDrift
  }

  val velocity: PeriodicSignal[Value3D[AngularVelocity]] = Signal {
    if (calibrating) {
      Value3D[AngularVelocity](DegreesPerSecond(0), DegreesPerSecond(0), DegreesPerSecond(0))
    } else {
      retrieveVelocity + currentDrift
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
