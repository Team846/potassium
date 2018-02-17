package com.lynbrookrobotics.potassium.vision

import com.lynbrookrobotics.potassium.Signal
import com.lynbrookrobotics.potassium.streams.Stream
import com.lynbrookrobotics.potassium.vision.limelight.VisionProperties
import squants.{Dimensionless, Length}
import squants.space.Angle

object VisionTargetTracking {
  def distanceToTarget(percentArea: Stream[Option[Dimensionless]], camProps: Signal[VisionProperties]): Stream[Option[Length]] = {
    percentArea.map ( p =>
      p.map { percentArea =>
        camProps.get.distanceConstant / math.sqrt(percentArea.toPercent)
      }
    )
  }

  def angleToTarget(xOffset: Stream[Angle], camProps: Signal[VisionProperties]): Stream[Angle] = {
    xOffset.map(-_ - camProps.get.cameraHorizontalOffset)
  }
}
