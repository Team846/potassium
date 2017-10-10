package com.lynbrookrobotics.potassium.commons.drivetrain

import com.lynbrookrobotics.potassium.units.{Point, Segment}
import squants.{Dimensionless, Each, Percent}
import squants.space.{Feet, Length, Radians}


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

    val dr_squared = (segment.length * segment.length).toSquareFeet

    val det = (diffStart.x * diffEnd.y - diffEnd.x * diffStart.y).toSquareFeet
    val discriminant = dr_squared * radius.toFeet * radius.toFeet - det * det

    if (dr_squared == 0) {
      throw new IllegalArgumentException("Segment is a point, so no line can be fit through it")
    }

    if (discriminant < 0) None else {
      val dy = segment.dy.toFeet
      val dx = segment.dx.toFeet

      val posX = center.x.toFeet
      val posY = center.y.toFeet

      val sqrtDiscrim = sqrt(discriminant)
      val signDy = if (dy < 0) -1D else 1D

      val positiveSolution = new Point(
        Feet((det * dy + signDy * sqrtDiscrim * dx) / dr_squared + posX),
        Feet((-det * dx + abs(dy) * sqrtDiscrim) / dr_squared + posY)
      )

      val negativeSolution = new Point(
        Feet((det * dy - signDy * sqrtDiscrim * dx) / dr_squared + posX),
        Feet((-det * dx - abs(dy) * sqrtDiscrim) / dr_squared + posY)
      )

      Some(negativeSolution, positiveSolution)
    }
  }

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
  def intersectionLineCircleFurthestFromStart(segment: Segment,
                                              center: Point,
                                              radius: Length): Option[Point] = {
    val solutions = interSectionCircleLine(segment, center, radius)
    solutions.flatMap {case(positive, negative) =>
        val positiveDiffWithStart = segment.start distanceTo positive
        val negativeDiffWithStart = segment.start distanceTo negative

        val furthestFromStart = if ( positiveDiffWithStart > negativeDiffWithStart ) {
          positive
        } else {
          negative
        }
        val closerToStart = if ( positiveDiffWithStart <= negativeDiffWithStart ) {
          positive
        } else {
          negative
        }
        val tolerance = Radians(0.0001)

        // Pick point such that look ahead point angle to end point is same as angle
        // of segment, ensuring that we drive toward the correct direction
        if ((segment.angle - Segment(segment.start, furthestFromStart).angle).abs < tolerance ) {
          Some(furthestFromStart)
        } else if ( (segment.angle - Segment(segment.start, closerToStart).angle).abs < tolerance ) {
          Some(closerToStart)
        } else {
          None
        }
    }
  }

  def limitCurrentOutput(input: Dimensionless, normalizedV: Dimensionless,
                         forwardCurrentLimit: Dimensionless,
                         backwardsCurrentLimit: Dimensionless): Dimensionless = {
    if(normalizedV < Each(0)) {
      -limitCurrentOutput(-input, -normalizedV, forwardCurrentLimit, backwardsCurrentLimit)
    }
    if(input > normalizedV){
      input.min(normalizedV + forwardCurrentLimit)
    } else if(input < Each(0)){
      val limitedInput = Each(-backwardsCurrentLimit / (Each(1) + normalizedV))
      limitedInput.max(input)
    } else {
      input
    }
  }
}
