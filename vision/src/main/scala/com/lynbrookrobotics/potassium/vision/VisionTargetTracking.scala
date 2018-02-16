package com.lynbrookrobotics.potassium.vision

import com.lynbrookrobotics.potassium.Signal
import com.lynbrookrobotics.potassium.streams.Stream
import squants.Dimensionless
import squants.space.{Angle, Length}

class VisionTargetTracking(cameraHorizontalOffset: Signal[Angle], distanceConstant: Signal[Length]) {
  def distanceToTarget(percentArea: Stream[Option[Dimensionless]]): Stream[Option[Length]] = {
        percentArea.map ( p =>
          p.map { percentArea =>
            distanceConstant.get / math.sqrt(percentArea.toPercent)
          }
        )
  }

  def angleToTarget(xOffset: Stream[Angle]): Stream[Angle] = {
    xOffset.map(p => -(p + cameraHorizontalOffset.get))
  }
}
