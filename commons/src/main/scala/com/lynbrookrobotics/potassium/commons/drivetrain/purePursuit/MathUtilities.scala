package com.lynbrookrobotics.potassium.commons.drivetrain.purePursuit

import com.lynbrookrobotics.potassium.units.{Point, Segment}
import squants.space.{Degrees, Feet, Length, Radians}
import squants.{Angle, Dimensionless}


object MathUtilities {
  /**
    * see http://mathworld.wolfram.com/Circle-LineIntersection.html
    * @param segment segment defining the infinite line to test for intersection
    * @param center center of circle to test for intersection
    * @param radius radius of circle to test for intersection
    * @return on Option of a tuple of points where the
    *         infinitely long line and circle intersect
    */
  def interSectionCircleLine(segment: Segment,
                             center: Point,
                             radius: Length): Option[(Point, Point)] = {
    import math._
    val diffEnd   = segment.end - center
    val diffStart = segment.start - center

    val drSquared = (segment.length * segment.length).toSquareFeet

    val det = (diffStart.x * diffEnd.y - diffEnd.x * diffStart.y).toSquareFeet
    val discriminant = drSquared * radius.toFeet * radius.toFeet - det * det

    if (drSquared == 0) {
      throw new IllegalArgumentException("Segment is a point, so no line can be fit through it")
    }

    if (discriminant < 0) None else {
      val dy = segment.dy.toFeet
      val dx = segment.dx.toFeet

      val posX = center.x.toFeet
      val posY = center.y.toFeet

      val sqrtDiscrim = sqrt(discriminant)
      val signDy = if (dy < 0) -1D else 1D

      val positiveSolution = Point(
        Feet((det * dy + signDy * sqrtDiscrim * dx) / drSquared + posX),
        Feet((-det * dx + abs(dy) * sqrtDiscrim) / drSquared + posY)
      )

      val negativeSolution = Point(
        Feet((det * dy - signDy * sqrtDiscrim * dx) / drSquared + posX),
        Feet((-det * dx - abs(dy) * sqrtDiscrim) / drSquared + posY)
      )

      Some(negativeSolution, positiveSolution)
    }
  }

  /**
    * Finds the intersection of the ray defined by segment and the circle defined by center and radius.
    * To acheive this, the intersection with line and circle are passed through the following tests:
    *
    * If that point's angle formed with the start point is
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
  def intersectionRayCircleFurthestFromStart(segment: Segment,
                                             center: Point,
                                             radius: Length): Option[Point] = {
    val solutions = interSectionCircleLine(segment, center, radius)
    solutions.flatMap { case (positive, negative) =>
        val positiveDiffWithStart = segment.start distanceTo positive
        val negativeDiffWithStart = segment.start distanceTo negative

        val furtherFromStart = if ( positiveDiffWithStart > negativeDiffWithStart ) {
          positive
        } else {
          negative
        }
        val closerToStart = if ( positiveDiffWithStart <= negativeDiffWithStart ) {
          positive
        } else {
          negative
        }

        // Pick point such that look ahead point angle to end point is same as angle
        // of segment, ensuring that we drive toward the correct direction
        val tolerance = Radians(0.0001)
        if ((segment.angle - Segment(segment.start, furtherFromStart).angle).abs < tolerance ) {
          Some(furtherFromStart)
        } else if ( (segment.angle - Segment(segment.start, closerToStart).angle).abs < tolerance ) {
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
    * @param trigonemtricAngle
    * @return
    */
  def swapTrigonemtricAndCompass(trigonemtricAngle: Angle): Angle = {
    Degrees(90) - trigonemtricAngle
  }

  def clamp[T](toClamp: Dimensionless, max: Dimensionless): Dimensionless = {
    if (toClamp.abs > max) {
      toClamp.value.signum * max
    } else {
      toClamp
    }
  }
}
