package com.lynbrookrobotics.potassium.commons.drivetrain.twoSided

import com.lynbrookrobotics.potassium.Signal
import com.lynbrookrobotics.potassium.streams.Stream
import com.lynbrookrobotics.potassium.units.{Line, Point}
import squants.Angle
import squants.space.{Inches, Length}

object BusDriving {
  def getTurnRadius(steeringAngle: Stream[Angle])(implicit props: Signal[TwoSidedDriveProperties]): Stream[Length] = {
    val halfLengthOfRobot = props.get.robotLength / 2
    steeringAngle.map { angle: Angle => {}
      val line = Line(angle, halfLengthOfRobot)
      (Point(line.xIntercept, Inches(0)) - Point(Inches(0), halfLengthOfRobot)).magnitude * line.xIntercept.value.signum
    }
  }
}
