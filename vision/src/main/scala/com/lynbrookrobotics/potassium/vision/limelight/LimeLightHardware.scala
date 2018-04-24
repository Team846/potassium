package com.lynbrookrobotics.potassium.vision.limelight

import com.lynbrookrobotics.potassium.Signal
import com.lynbrookrobotics.potassium.clock.Clock
import com.lynbrookrobotics.potassium.streams._
import com.lynbrookrobotics.potassium.vision.{VisionProperties, VisionTargetTracking}
import edu.wpi.first.networktables.NetworkTableInstance
import squants.Time
import squants.space.{Degrees, Feet}

class LimeLightHardware(upright: Boolean)(implicit clock: Clock, props: Signal[VisionProperties]) {
  val limelightInterface = LimelightNetwork(clock)
  val tracker = new VisionTargetTracking(props)

  val distanceToTarget = tracker.distanceToTarget(limelightInterface.percentArea)

  val angleToTarget = if (upright) {
    tracker.compassAngleToTarget(limelightInterface.xOffsetAngle)
  } else {
    tracker.compassAngleToTarget(limelightInterface.yOffsetAngle)
  }

  val hasTarget = limelightInterface.hasTarget

  limelightInterface.table.getEntry("ledMode").setDouble(1)
}
