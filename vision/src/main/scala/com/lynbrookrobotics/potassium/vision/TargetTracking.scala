package com.lynbrookrobotics.potassium.vision

import com.lynbrookrobotics.potassium.streams.Stream
import com.lynbrookrobotics.potassium.units.Point
import squants.{Dimensionless, Length}
import squants.space.{Angle, Degrees, Feet, Inches}
import squants.time.Milliseconds
import com.lynbrookrobotics.potassium.vision.limelight.LimelightNetwork._

class TargetTracking {

  val cameraHorizontalOffset: Angle = Degrees(0)
  val cameraVerticalOffset: Angle = Degrees(0)
  val cameraHeight: Length = Feet(0)
  val targetHeight: Length = Inches(11)

  val target: Stream[Point] = Stream.periodic(Milliseconds(5)){
    xOffsetAngle.zip(yOffsetAngle).zip(targetArea).zip(targetSkew).map { case (((xOffsetAngle: Angle, yOffsetAngle: Angle),
    targetArea: Dimensionless), targetSkew: Dimensionless) => {
      val d: Length = Feet((Feet(targetHeight).value - cameraHeight.value) / math.tan(cameraVerticalOffset.value + yOffsetAngle.value))
      val ang: Angle = Degrees(xOffsetAngle.value - cameraHorizontalOffset.value)

      Point(x = d * math.cos(ang.value),y = d * math.cos(ang.value))
    }
    }
  }
}
