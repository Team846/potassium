package com.lynbrookrobotics.potassium.vision

import com.lynbrookrobotics.potassium.streams.Stream
import com.lynbrookrobotics.potassium.units.Point
import com.lynbrookrobotics.potassium.vision.limelight.{CameraProperties, LimelightNetwork}
import squants.Length
import squants.space.{Angle, Degrees, Feet}


class TargetTracking(implicit limelightNetwork: LimelightNetwork, props: CameraProperties) {
  val target: Stream[Point] = {
    limelightNetwork.xOffsetAngle.zip(limelightNetwork.yOffsetAngle).map { case (xOffsetAngle: Angle, yOffsetAngle: Angle) => {
        val d: Length = Feet(props.targetHeight.value - props.cameraHeight.value) / math.tan(props.cameraVerticalOffset.value + yOffsetAngle.value)
        val ang: Angle = Degrees(xOffsetAngle.value - props.cameraHorizontalOffset.value)

        Point(x = d * math.cos(ang.value),y = d * math.cos(ang.value))
      }
    }
  }
}
