package com.lynbrookrobotics.potassium.commons.drivetrain.unicycle.control.purePursuit

import com.lynbrookrobotics.potassium.units.{Point, Segment}
import squants.space.{Degrees, Length, Radians}
import squants.{Angle, Dimensionless}

object MathUtilities {

  /**
   * This finds the point furthest from the start and if that point's angle formed with the start point is
   * the same as the angle of the segment, it returns that point.
   *
   * If the point furthest of the start doesn't meet the angle condition, it checks if the other point meets it and
   * returns that point if it matches the angle condition
   *
   * If both points don't meet the angle condition or there aren't any intersections, then this method returns None
   * @param segment the segment that the circle intersects
   * @param center the center of the circle that intersects the segment
   * @param radius the radius of the circle that intersects
   * @return
   */
  def intersectionRayCircleFurthestFromStart(segment: Segment, center: Point, radius: Length): Option[Point] = {
    val solutions = segment.intersectionWithCircle(center, radius)

    solutions.flatMap {
      case (positive, negative) =>
        val positiveDiffWithStart = segment.start distanceTo positive
        val negativeDiffWithStart = segment.start distanceTo negative

        val (furthestFromStart, closerToStart) = if (positiveDiffWithStart > negativeDiffWithStart) {
          (positive, negative)
        } else {
          (negative, positive)
        }

        val onLine = segment.pointClosestToOnLine(center)

        implicit val tolerance: Angle = Degrees(45) //Radians(0.0001)

        val angleRobotProjectionToEnd = Segment(onLine, segment.end).angle
        val hasOvershot = !(angleRobotProjectionToEnd ~= segment.angle)

        // Pick point such that angle from the robot location projected on the segment to the end is the same
        // as the robot location projection to the look ahead point, ensuring that we drive toward the correct direction.
        // We also make sure the angle from the start to end equals the angle from start to look ahead point, ensuring
        // that we don't extrapolate behind the start.
        if ((angleRobotProjectionToEnd ~= Segment(onLine, furthestFromStart).angle) &&
            (segment.angle ~= Segment(segment.start, furthestFromStart).angle)) {
          Some(furthestFromStart)
        } else if ((angleRobotProjectionToEnd ~= Segment(onLine, closerToStart).angle) &&
                   ((segment.angle ~= Segment(segment.start, closerToStart).angle) || hasOvershot)) {
          Some(closerToStart)
        } else {
          None
        }
    }
  }

  /**
   * swapps a trigometric angle, as used in the mathematics
   * (right is 0 degrees, increasing counter clockwise) to
   * compass angle, forward is 0, increases clockwise, or vice versa
   * @param trigonometricAngle
   * @return
   */
  def convertTrigonometricAngleToCompass(trigonometricAngle: Angle): Angle = {
    Degrees(90) - trigonometricAngle
  }

  def clamp[T](toClamp: Dimensionless, max: Dimensionless): Dimensionless = {
    if (toClamp.abs > max) {
      toClamp.value.signum * max
    } else {
      toClamp
    }
  }
}
