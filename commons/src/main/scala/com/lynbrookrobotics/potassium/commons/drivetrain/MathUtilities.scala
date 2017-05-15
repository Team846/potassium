package com.lynbrookrobotics.potassium.commons.drivetrain

import com.lynbrookrobotics.potassium.units.{Point, Segment}
import squants.{Dimensionless, Each}
import squants.space.{Feet, Length}


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
    import Math._
    val diffEnd   = segment.end - center
    val diffStart = segment.start - center

    val dr_squared = (segment.length * segment.length).toSquareFeet

    val det = (diffStart.x * diffEnd.y - diffEnd.x * diffStart.y).toSquareFeet
    val discriminant = dr_squared * radius.toFeet * radius.toFeet - det * det

    if (dr_squared == 0) {
      throw new IllegalArgumentException("Segment is a point, so no line can be fit through it. Segment" +
        s"$segment center $center radius $radius")
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
    * Finds intersection between segment and circle with given center and radius
    * lookAheadDistance that is closest to segment.end
    * @param segment
    * @param center
    * @param radius
    * @return
    */
  def intersectionClosestToEnd(segment: Segment,
                               center: Point,
                               radius: Length): Option[Point] = {
    interSectionCircleLine(segment, center, radius).flatMap { case (negativeSolution, positiveSolution) =>
      val negSolutionLengthToEnd = negativeSolution distanceTo segment.end
      val posSolutionLengthToEnd = positiveSolution distanceTo segment.end

      val solutionClosestToEnd = if (posSolutionLengthToEnd >= negSolutionLengthToEnd) {
        negativeSolution
      } else {
        positiveSolution
      }

      Some(solutionClosestToEnd)
//      // Intersection is found between the LINE formed by the start and end
//      // point of given segment. This means that the given solution might
//      // extend past end point of segment. In this case, just return the end
//      if (segment.containsInXY(solutionClosestToEnd, Feet(0.2))) {
//        Some(solutionClosestToEnd)
//      } else {
//        Some(segment.end)
//      }
    }
  }

  def intersectionFurthestFromStart(segment: Segment,
                                              center: Point,
                                              radius: Length): Option[Point] = {
    val solutions = interSectionCircleLine(segment, center, radius)
    solutions.flatMap { s =>
      val (negative, positive) = s

      implicit val Tolerance = Feet(0.001)
      val negativeToStart = negative distanceTo segment.start
      val positiveToStart = positive distanceTo segment.start

      // Handle edge case where both solutions are equidistant from start. Then
      // return solution closest to the endpoint of the segment
      if (negativeToStart ~= positiveToStart) {
        val negativeToEnd = negative distanceTo segment.end
        val positiveToEnd = positive distanceTo segment.end

        if (positiveToEnd < negativeToEnd) {
          Some(positive)
        } else {
          Some(negative)
        }
      } else if (negativeToStart > positiveToStart) {
        Some(negative)
      } else {
        Some(positive)
      }
    }
  }

  def optimalPurePursuitInterSection(segment: Segment,
    center: Point,
    radius: Length): Option[Point] = {
    val toStart = center distanceTo segment.start
    val toEnd   = center distanceTo segment.end

    // TODO: Include detailed explanations, with diagrams, explaining how this
    // TODO: handles edge cases. THESE ARE NOT INDENTICAL!!!
    if (toStart < toEnd) {
      intersectionClosestToEnd(segment, center, radius)
    } else {
      intersectionFurthestFromStart(segment, center, radius)
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
