package com.lynbrookrobotics.potassium.vision

import com.lynbrookrobotics.potassium.streams.Stream
import com.lynbrookrobotics.potassium.vision.limelight.CameraProperties
import squants.{Dimensionless, Length}
import squants.space.{Angle, Feet}

class TargetTracking(val xOffset: Stream[Angle], val percentArea: Stream[Option[Dimensionless]])(implicit props: CameraProperties) {
  val distanceToTarget: Stream[Option[Length]] = {
    percentArea.map(r => {
      r.map { pct =>
        Feet(10.6845 / math.sqrt(pct.toPercent))
      }
    })
  }

  val angleToTarget: Stream[Angle] = {
    xOffset.map(p => p + props.cameraHorizontalOffset)
  }
}