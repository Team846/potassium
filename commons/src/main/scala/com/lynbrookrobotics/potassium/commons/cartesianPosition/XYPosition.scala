package com.lynbrookrobotics.potassium.commons.cartesianPosition

import com.lynbrookrobotics.potassium.units.Point
import com.lynbrookrobotics.potassium.streams.Stream
import squants.Angle
import squants.motion.Velocity
import squants.space._

object XYPosition {

  /**
   * returns Stream of current xy position provided distance traveled
   * and current angle. It assumes that motion of the robot is a straight line
   * between ticks, and then using vector addition to continually update
   * the position
   * @param angle current trigonometric angle in about z axis (in the xy
   *              plane), where 0 is defined at the x axis. Used as the angle of the
   *              vector in vector addition
   * @param distanceTraveled the total distance/arclength traveled. Used as the
   *                         magnitude of the vector used in vector addition
   * @return a stream of points of the robot's path

   */
  def apply(angle: Stream[Angle], distanceTraveled: Stream[Length]): Stream[Point] = {
    val averageAngle = angle.sliding(2).map(angles => (angles.head + angles.last) / 2D)

    val deltaDistance = distanceTraveled.sliding(2).map { distances =>
      distances.last - distances.head
    }

    // approximate that robot traveled in a straight line at the average
    // angle over the course of 1 tick
    deltaDistance.zip(averageAngle).scanLeft(Point.origin) {
      case (acc, (distance, avrgAngle)) =>
        acc + Point(distance * avrgAngle.cos, distance * avrgAngle.sin)
    }
  }

  /**
   * differentiates x and y position, then reintegrates with Simpsons integration
   * @param angle current trigonometric angle in about z axis (in the xy
   *              plane), where 0 is defined at the x axis. Used as the angle of the
   *              vector in vector addition
   * @param velocity a stream of the robot's velocity
   * @return a stream of points of the robot's path
   */
  def positionWithSimpsons(angle: Stream[Angle], velocity: Stream[Velocity]): Stream[Point] = {
    val averageAngle = angle.sliding(2).map(angles => (angles.head + angles.last) / 2D)

    val velocityX = velocity.zip(averageAngle).map {
      case (speed, avrgAngle) =>
        avrgAngle.cos * speed
    }

    val velocityY = velocity.zip(averageAngle).map {
      case (speed, avrgAngle) =>
        avrgAngle.sin * speed
    }

    val xPosition = velocityX.simpsonsIntegral
    val yPosition = velocityY.simpsonsIntegral

    xPosition.zip(yPosition).map {
      case (xPose, yPose) =>
        Point(xPose, yPose)
    }
  }

  /**
   * tracks the position of the robot by assuming a circular motion between every tick,
   * to create a more realistic model of the robot's angular velocity
   * @param angle current trigonometric angle in about z axis (in the xy
   *              plane), where 0 is defined at the x axis. Used as the angle of the
   *              vector in vector addition
   * @param distanceTraveled the distance traveled. Used to calculate the
   *                         length of the circular arc between every tick
   * @return stream of points of the robot's path
   */
  def circularTracking(angle: Stream[Angle], distanceTraveled: Stream[Length]): Stream[Point] = {
    val centralAngle = angle.sliding(2).map(angleQueue => angleQueue.last - angleQueue.head)
    val arcLength = distanceTraveled.sliding(2).map(arcQueue => arcQueue.last - arcQueue.head)
    val radius = centralAngle.zip(arcLength).map {
      case (centralAngle, arcLength) =>
        arcLength / centralAngle.toRadians
    }

    val previousAngle = angle.sliding(2).map(_.apply(1))

    centralAngle.zip(previousAngle).zip(radius).zip(arcLength).zip(angle).scanLeft(Point.origin) {
      case (prevPos, ((((centralAngle, prevAngle), radius), arcLength), currAngle)) =>
        // if centralAngle = 0, the motion is in a circle of infinite radius,
        // which is just traveling in a straight line
        if (centralAngle.toDegrees == 0.0) {
          Point(x = prevPos.x + arcLength * currAngle.cos, y = prevPos.y + arcLength * currAngle.sin)
        } else {
          // robot angle is tangential to the radius of the circular arc traveled,
          // so add 90 degrees to get the angle of the radius
          val center = Point(
            x = prevPos.x + radius * (prevAngle + Degrees(90)).cos,
            y = prevPos.y + radius * (prevAngle + Degrees(90)).sin
          )
          prevPos.rotateAround(center, centralAngle)
        }
    }
  }
}
