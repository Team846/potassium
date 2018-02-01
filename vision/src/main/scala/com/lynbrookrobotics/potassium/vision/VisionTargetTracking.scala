package com.lynbrookrobotics.potassium.vision

import com.lynbrookrobotics.potassium.Signal
import com.lynbrookrobotics.potassium.streams.Stream
import com.lynbrookrobotics.potassium.vision.limelight.VisionProperties
import squants.{Dimensionless, Length}
import squants.space.Angle

object VisionTargetTracking {
  def distanceToTarget(percentArea: Stream[Option[Dimensionless]])
                      (implicit props: Signal[VisionProperties]): Stream[Option[Length]] = {
    percentArea.map ( p =>
      p.map { percentArea =>
        props.get.distanceConstant / math.sqrt(percentArea.toPercent)
      }
    )
  }

  def angleToTarget(xOffset: Stream[Angle])
                   (implicit props: Signal[VisionProperties]): Stream[Angle] = {
    xOffset.map(-_ - props.get.cameraHorizontalOffset)
  }
}
