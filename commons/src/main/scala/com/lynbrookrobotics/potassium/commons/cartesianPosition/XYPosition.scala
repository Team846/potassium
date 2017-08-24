package com.lynbrookrobotics.potassium.commons.cartesianPosition

import com.lynbrookrobotics.potassium.clock.Clock
import com.lynbrookrobotics.potassium.units.Point
import com.lynbrookrobotics.potassium.streams.Stream
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
  def apply(angle: Stream[Angle],
            distanceTraveled: Stream[Length]): Stream[Point] = {
    val averageAngle = angle.sliding(2).map(angles =>
      (angles.head + angles.last) / 2D)

    val deltaDistance = distanceTraveled.sliding(2).map { distances =>
      distances.last - distances.head
    }

    // approximate that robot traveled in a straight line at the average
    // angle over the course of 1 tick
    deltaDistance.zip(averageAngle).scanLeft(Point.origin){
      case (acc, (distance, avrgAngle)) =>
        acc + Point(
          distance * avrgAngle.cos,
          distance * avrgAngle.sin)
    }
  }

  /**
    * differentiats x and y position, then reintegrates with Simpsons integration
    * @param angle
    * @param distanceTraveled
    * @return
    */
  def positionWithSimpsons(angle: Stream[Angle],
                           distanceTraveled: Stream[Length]): Stream[Point] = {
    val averageAngle = angle.sliding(2).map(angles =>
      (angles.head + angles.last) / 2D)

    val velocity = distanceTraveled.derivative
    val velocityX = velocity.zip(averageAngle).map{ case(speed, avrgAngle) =>
      avrgAngle.cos * speed
    }

    val velocityY = velocity.zip(averageAngle).map{ case(speed, avrgAngle)  =>
      avrgAngle.sin * speed
    }

    val xPosition = velocityX.simpsonsIntegral
    val yPosition = velocityY.simpsonsIntegral

    xPosition.zip(yPosition).map { case (xPose, yPose) =>
      Point(xPose, yPose)
    }
  }
}