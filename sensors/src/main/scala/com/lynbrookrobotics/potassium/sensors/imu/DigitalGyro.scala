package com.lynbrookrobotics.potassium.sensors.imu

import com.lynbrookrobotics.potassium.units.Value3D

import squants.motion.{AngularVelocity, DegreesPerSecond}
import squants.Time

import scala.collection.mutable

/**
  * Calibration, calculation for velocity
  * @param tickPeriod tick period of robot
  * @param maxDriftDeviation maximum value the difference between drift and chunks that is allowed
  */
abstract class DigitalGyro(tickPeriod: Time, maxDriftDeviation: Value3D[AngularVelocity]) {
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
    * End the collection of values used to calibrate
    */
  def endCalibration(): Unit = {
    if (calibrating) {
      val sum = calibrationVelocities.reduceLeft { (acc, cur) =>
        acc + cur
      }
      currentDrift = sum * (1D / calibrationVelocities.size)
      calibrating = false
    }
  }

  def getVelocities: Value3D[AngularVelocity] = {
    if (calibrating) {
      calibrationVelocities.enqueue(retrieveVelocity)

      if (calibrationVelocities.size > 1000) calibrationVelocities.dequeue()

      Value3D(DegreesPerSecond(0), DegreesPerSecond(0), DegreesPerSecond(0))
    } else {
      retrieveVelocity - currentDrift
    }
  }

  def checkCalibrationValid: Boolean = {
    val chunks: Seq[Seq[Value3D[AngularVelocity]]] = {
      val part1 = calibrationVelocities.splitAt(calibrationVelocities.size / 5)
      val part2 = part1._2.splitAt(calibrationVelocities.size / 5)
      val part3 = part2._2.splitAt(calibrationVelocities.size / 5)
      val part4 = part3._2.splitAt(calibrationVelocities.size / 5)
      Seq(part1._1.toList, part2._1.toList, part3._1.toList, part4._1.toList, part4._2.toList)
    }
    
    chunks.forall { chunk =>
      val sumChunk = chunk.reduceLeft{ (acc, cur) =>
        acc + cur
      }
      val average = sumChunk * (1D / chunk.size)
      ((average.x - currentDrift.x).abs <= maxDriftDeviation.x) &&
        ((average.y - currentDrift.y).abs <= maxDriftDeviation.y) &&
        ((average.z - currentDrift.z).abs <= maxDriftDeviation.z) &&
        (calibrationVelocities.size() == 1000)
    }
  }
}
