package com.lynbrookrobotics.potassium.sensors.position

import com.lynbrookrobotics.potassium.units.Point
import com.lynbrookrobotics.potassium.{PeriodicSignal, Signal}
import squants.Angle
import squants.space._
import squants.time.{Milliseconds, Seconds}

object xyPosition {
  /**
    * returns signal of current xy position provided distance traveled since
    * start of calculations and current angle
    * @param initPosition initial position
    * @param angle current trigonometric angle in about z axis (in the xy
    *              plane), where 0 is defined at the x axis
    * @param distanceTraveled the total distance/arclength traveled
    */
  def apply(initPosition: Point,
            angle: Signal[Angle],
            distanceTraveled: PeriodicSignal[Length]): PeriodicSignal[Point] = {
    val initAngle    = angle.get
    val averageAngle = angle.toPeriodic.sliding(2, initAngle).map(angles =>
      (angles.head + angles.tail.head) / 2D)

    val deltaDistance = distanceTraveled.sliding(2, Feet(0)).map(distances =>
      distances.last - distances.front)

    deltaDistance.zip(averageAngle).scanLeft(initPosition){
      case (acc, (distance, avrgAngle), _) =>
        println("delta distance: ", distance.toFeet)
        println(s"average angle: ${avrgAngle.toDegrees}")
        println(s"accum: $acc")
        acc + new Point(
          distance * Math.cos(avrgAngle.toRadians),
          distance * Math.sin(avrgAngle.toRadians)
        )
    }
  }
}