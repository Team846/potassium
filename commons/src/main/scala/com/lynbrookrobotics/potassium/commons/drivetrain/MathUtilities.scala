package com.lynbrookrobotics.potassium.commons.drivetrain

import com.lynbrookrobotics.potassium.commons.{Point, Segment}
import squants.space.{Feet, Length}


object MathUtilities {
  // TODO: Add source for equations
  def interSectionCircleLine(segment: Segment,
                             center: Point,
                             radius: Length): Option[(Point, Point)] = {
    import Math._
    val diffEnd   = segment.end - center
    val diffStart = segment.start - center

    val dr_squared = (segment.length * segment.length).toSquareFeet

    val det = (diffStart.x * diffEnd.y - diffEnd.x * diffStart.y).toSquareFeet
    val discriminant = dr_squared * radius.toFeet * radius.toFeet - det * det

    if (discriminant < 0) None
    else {
      val dy = segment.dy.toFeet
      val dx = segment.dx.toFeet

      val posX = center.x.toFeet
      val posY = center.y.toFeet

      val sqrtDiscrim = sqrt(discriminant)
      val signDy = signum(dy)

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
    * lookAheadDistance that is closest to segment.end.
    * @param segment
    * @param center
    * @param radius
    * @return
    */
  def intersectionClosestToEnd(segment: Segment,
                               center: Point,
                               radius: Length): Option[Point] = {
    interSectionCircleLine(segment, center, radius).flatMap {
      case (negativeSolution, positiveSolution) =>
        val negSolutionLengthToEnd = negativeSolution distanceTo segment.end
        val posSolutionLengthToEnd = positiveSolution distanceTo segment.end

        val solutionClosestToEnd = {
          if (posSolutionLengthToEnd >= negSolutionLengthToEnd) positiveSolution
          else negativeSolution
        }

        // Intersection is found between the LINE formed by the start and end
        // point of given segment. This means that the given solution might
        // extend past end point of segment. In this case, just return the end
        if (segment containsInXY solutionClosestToEnd) {
          Some(solutionClosestToEnd)
        }
        else Some(segment.end)
    }
  }
}
