package com.lynbrookrobotics.potassium.commons.cartesianPosition

import com.lynbrookrobotics.potassium.units.Point
import com.lynbrookrobotics.potassium.{PeriodicSignal, Signal}
import squants.Angle
import squants.space._

object XYPosition {
  /**
    * returns signal of current xy position provided distance traveled since
    * start of calculations and current angle. It assumes that motion of the robot is
    * a straight line between ticks, and then using vector addition to continually update
    * the position
    * @param angle current trigonometric angle in about z axis (in the xy
    *              plane), where 0 is defined at the x axis. Used as the angle of the
    *              vector in vector addition
    * @param distanceTraveled the total distance/arclength traveled. Used as the
    *                         magnitude of the vectore used in vector addition
    */
  def apply(angle: Signal[Angle],
            distanceTraveled: Signal[Length]): PeriodicSignal[Point] = {
    val initAngle = angle.get
    val averageAngle = angle.toPeriodic.sliding(2, initAngle).map(angles =>
      (angles.head + angles.last) / 2D)

    val initDistanceTraveled = distanceTraveled.get
    val deltaDistance = distanceTraveled.toPeriodic.sliding(2, initDistanceTraveled).map { distances =>
      distances.last - distances.head
    }

    val origin = Point(
      Feet(0),
      Feet(0))

    // approximate that robot traveled in a straight line at the average
    // angle over the course of 1 tick
    deltaDistance.zip(averageAngle).scanLeft(origin){
      case (acc, (distance, avrgAngle), _) =>
        acc + Point(
          distance * avrgAngle.cos,
          distance * avrgAngle.sin)
    }
  }

  /**
    * differentiats position, then reintegrates with Simpsons integration
    * @param angle
    * @param distanceTraveled
    * @return
    */
  def positionWithSimpsons(angle: Signal[Angle],
                           distanceTraveled: Signal[Length]): PeriodicSignal[Point] = {
    val initAngle = angle.get
    val averageAngle = angle.toPeriodic.sliding(2, initAngle).map(angles =>
      (angles.head + angles.last) / 2D)

    val velocity = distanceTraveled.toPeriodic.derivative
    val velocityX = velocity.zip(averageAngle).map{v =>
      val (speed, angle) = v
      angle.cos * speed
    }

    val velocityY = velocity.zip(averageAngle).map{v =>
      val (speed, angle) = v
      angle.sin * speed
    }

    val xPosition = velocityX.simpsonsIntegral
    val yPosition = velocityY.simpsonsIntegral

    xPosition.zip(yPosition).map { pose =>
      Point(pose._1, pose._2)
    }
  }
}