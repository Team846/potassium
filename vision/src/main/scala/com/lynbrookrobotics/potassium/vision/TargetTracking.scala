package com.lynbrookrobotics.potassium.vision

import com.lynbrookrobotics.potassium.streams.Stream
import com.lynbrookrobotics.potassium.vision.limelight.CameraProperties
import squants.Length
import squants.space.{Angle, Feet}


class TargetTracking(val offsets: Stream[(Angle, Angle)])(implicit props: CameraProperties) {
  def target: Stream[(Length, Angle)] = {
    offsets.map { case (xOffsetAngle: Angle, yOffsetAngle: Angle) =>
      val ratio: Double = (props.cameraVerticalOffset + yOffsetAngle).tan
      val d: Length = Feet(props.targetHeight.value - props.cameraHeight.value) / ratio
      val ang: Angle = xOffsetAngle + props.cameraHorizontalOffset
      (d, ang)
    }
  }
}
