package com.lynbrookrobotics.potassium.vision.limelight

import com.lynbrookrobotics.potassium.Signal
import com.lynbrookrobotics.potassium.clock.Clock
import com.lynbrookrobotics.potassium.streams._
import com.lynbrookrobotics.potassium.vision.{VisionProperties, VisionTargetTracking}
import edu.wpi.first.networktables.NetworkTableInstance
import squants.space.{Degrees, Feet}


class LimeLightHardware(implicit clock: Clock) {
  while (!NetworkTableInstance.getDefault.getTable("/limelight").getEntry("tv").exists()) {
    // busy wait until network table is initialized
  }

  val limelightInterface = LimelightNetwork(clock)
  val tracker = new VisionTargetTracking(
    Signal.constant(VisionProperties(Degrees(0), Feet(10.8645))))

  val distanceToTarget = tracker.distanceToTarget(limelightInterface.percentArea)
  val angleToTarget = tracker.compassAngleToTarget(limelightInterface.xOffsetAngle)
  val hasTarget = limelightInterface.hasTarget

  limelightInterface.table.getEntry("ledMode").setDouble(1)
}
