package com.lynbrookrobotics.potassium.vision.limelight

import com.lynbrookrobotics.potassium.Signal
import com.lynbrookrobotics.potassium.clock.Clock
import com.lynbrookrobotics.potassium.streams._
import com.lynbrookrobotics.potassium.vision.VisionTargetTracking
import edu.wpi.first.networktables.NetworkTableInstance
import squants.space.{Angle, Degrees, Feet, Length}

abstract class CameraHardware {
  val distanceToTarget: Stream[Option[Length]]
  val angleToTarget: Stream[Angle]
  val hasTarget: Stream[Boolean]
}

class LimeLightHardware(cameraAngleRelativeToFront: Signal[Angle],
                        reciprocalRootAreaToDistanceConversion: Signal[Length])
                       (implicit clock: Clock) extends CameraHardware {
  while (!NetworkTableInstance.getDefault.getTable("/limelight").getEntry("tv").exists()) {
    // busy wait until network table is initialized
  }

  val limelightInterface = LimelightNetwork(clock)
  val tracker = new VisionTargetTracking(
    cameraHorizontalOffset = Signal.constant(Degrees(0)),
    distanceConstant = Signal.constant(Feet(10.8645)))

  val distanceToTarget = tracker.distanceToTarget(limelightInterface.percentArea)
  val angleToTarget = tracker.compassAngleToTarget(limelightInterface.xOffsetAngle)
  val hasTarget = limelightInterface.hasTarget

  limelightInterface.table.getEntry("ledMode").setDouble(1)
}
