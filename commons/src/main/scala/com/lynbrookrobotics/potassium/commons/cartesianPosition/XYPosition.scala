package com.lynbrookrobotics.potassium.commons.cartesianPosition

import com.lynbrookrobotics.potassium.units.Point
import com.lynbrookrobotics.potassium.{PeriodicSignal, Signal}
import squants.Angle
import squants.space._

object XYPosition {
//  /**
//    * returns signal of current xy position provided distance traveled since
//    * start of calculations and current angle
//    * @param angle current trigonometric angle in about z axis (in the xy
//    *              plane), where 0 is defined at the x axis
//    * @param distanceTraveled the total distance/arclength traveled
//    */
//  def apply(angle: Signal[Angle],
//            distanceTraveled: Signal[Length]): PeriodicSignal[Point] = {
////    val averageAngle = angle.toPeriodic.sliding(2, initAngle).map(angles =>
////      (angles.head + angles.last) / 2D)
//
//    val initDistanceTraveled = distanceTraveled.get
//    val deltaDistance = distanceTraveled.toPeriodic.
//      sliding(2, initDistanceTraveled).map { distances =>
//      distances.last - distances.head
//    }
//
//    val origin = new Point(
//      Feet(0),
//      Feet(0)
//    )
//
//    // approximate that robot traveled in a straight line at the average
//    // angle over the course of 1 tick
//    deltaDistance.zip(angle).scanLeft(origin){
//      case (acc, (distance, currentAngle), _) =>
//        acc + new Point(
//          distance * Math.cos(currentAngle.toRadians),
//          distance * Math.sin(currentAngle.toRadians))
//    }
//  }

  /**
    * returns signal of current xy position provided distance traveled since
    * start of calculations and current angle
    * @param angle current trigonometric angle in about z axis (in the xy
    *              plane), where 0 is defined at the x axis
    * @param distanceTraveled the total distance/arclength traveled
    */
  def apply(angle: Signal[Angle],
            distanceTraveled: Signal[Length]): PeriodicSignal[Point] = {
    val initAngle = angle.get
    val averageAngle = angle.toPeriodic.sliding(2, initAngle).map(angles =>
      (angles.head + angles.last) / 2D)

    val initDistanceTraveled = distanceTraveled.get
    val deltaDistance = distanceTraveled.toPeriodic.
      sliding(2, initDistanceTraveled).map { distances =>
      distances.last - distances.head
    }

    val origin = new Point(
      Feet(0),
      Feet(0)
    )

    // approximate that robot traveled in a straight line at the average
    // angle over the course of 1 tick
    deltaDistance.zip(averageAngle).scanLeft(origin){
      case (acc, (distance, avrgAngle), _) =>
        acc + new Point(
          distance * Math.cos(avrgAngle.toRadians),
          distance * Math.sin(avrgAngle.toRadians))
    }
  }
}