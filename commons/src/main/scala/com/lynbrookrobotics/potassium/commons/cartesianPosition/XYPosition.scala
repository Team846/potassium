package com.lynbrookrobotics.potassium.commons.cartesianPosition


import com.lynbrookrobotics.potassium.units.Point
import com.lynbrookrobotics.potassium.streams.Stream
import squants.Angle
import squants.motion.Velocity
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
    *                         magnitude of the vector used in vector addition
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
    * differentiates x and y position, then reintegrates with Simpsons integration
    * @param angle a stream of the angle of the robot
    * @param velocity a stream of the robot's velocity
    * @return a stream of points of the robot's path
    */
  def positionWithSimpsons(angle: Stream[Angle],
                           velocity: Stream[Velocity]): Stream[Point] = {
    val averageAngle = angle.sliding(2).map(angles =>
      (angles.head + angles.last) / 2D)

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

  /**
    * tracks the position of the robot by assuming a circular path in order to account for angular velocity
    * @param angle a stream of the angle of the robot
    * @param distanceTraveled a stream of the distance traveled by the robot
    * @return stream of points of the robot's path
    */
  def circularTracking(angle: Stream[Angle], distanceTraveled: Stream[Length]): Stream[Point] = {
    val centralAngle: Stream[Angle] = angle.sliding(2).map(angleQueue => angleQueue(1) - angleQueue(0))
    val arcLength: Stream[Length] = distanceTraveled.sliding(2).map(arcQueue => arcQueue(1) - arcQueue(0))
    val radius: Stream[Length] = centralAngle.zip(arcLength).map {
      case(centralAngle, arcLength) => arcLength / centralAngle.toRadians
    }

    val previousAngle: Stream[Angle] = angle.sliding(2).map(angleQueue => angleQueue(1))

    centralAngle.zip(previousAngle).zip(radius).zip(arcLength).zip(angle).scanLeft(Point.origin) {
      case(prevPos: Point,(((((centralAngle: Angle), previousAngle: Angle), radius: Length), arcLength: Length),angle: Angle)) =>
        if (centralAngle.value == 0.0) {
          Point(prevPos.x + angle.cos * arcLength, prevPos.y + arcLength * angle.sin)
        } else {
          val center: Point = Point(
            prevPos.x + radius * (previousAngle + Degrees(90)).cos,
            prevPos.y + radius * (previousAngle + Degrees(90)).sin)
          prevPos.rotateAround(center, centralAngle)
        }
    }
  }
}