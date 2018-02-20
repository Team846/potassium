package com.lynbrookrobotics.potassium.vision

import com.lynbrookrobotics.potassium.Signal
import com.lynbrookrobotics.potassium.streams.Stream
import squants.Dimensionless
import squants.space.{Angle, Length}

class VisionTargetTracking(props: Signal[VisionProperties]) {
  def distanceToTarget(percentArea: Stream[Option[Dimensionless]]): Stream[Option[Length]] = {
    percentArea.map ( p =>
      p.map { percentArea =>
        props.get.reciprocalRootAreaToDistanceConversion / math.sqrt(percentArea.toPercent)
      }
    )
  }

  def compassAngleToTarget(xOffset: Stream[Angle]): Stream[Angle] = {
    xOffset.map(p => p + props.get.cameraAngleRelativeToFront)
  }
}
