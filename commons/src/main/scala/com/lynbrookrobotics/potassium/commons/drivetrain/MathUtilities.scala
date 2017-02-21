package com.lynbrookrobotics.potassium.commons.drivetrain

import com.lynbrookrobotics.potassium.units.{Point, Segment}
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
    interSectionCircleLine(segment, center, radius).flatMap { case (negativeSolution, positiveSolution) =>
//      println(s"segment $segment, center $center radius $radius")
//      println(s"negative solution $negativeSolution positive solution $positiveSolution")
      val negSolutionLengthToEnd = negativeSolution distanceTo segment.end
      val posSolutionLengthToEnd = positiveSolution distanceTo segment.end

      val solutionClosestToEnd = if (posSolutionLengthToEnd >= negSolutionLengthToEnd) {
//        println(s"returning negative solution $negativeSolution")
        negativeSolution
      } else {
//        println(s"returning positive solution $positiveSolution")
        positiveSolution
      }

      // Intersection is found between the LINE formed by the start and end
      // point of given segment. This means that the given solution might
      // extend past end point of segment. In this case, just return the end
      if (segment.containsInXY(solutionClosestToEnd, Feet(0.2))) {
        Some(solutionClosestToEnd)
      } else {
//        println(s"returning end: ${segment.end}  segment $segment currpose $center solution $solutionClosestToEnd")
        Some(segment.end)
      }
    }
  }

  /**
    *
    * @param segment
    * @param center
    * @param radius
    * @return
    */
  def intersectionLineCircleFurthestFromStart(segment: Segment,
                                              center: Point,
                                              radius: Length): Option[Point] = {
    val solutions = interSectionCircleLine(segment, center, radius)
    solutions.flatMap { s =>
      val (positive, negative) = s
      if ((negative distanceTo segment.start) > (positive distanceTo segment.start)) {
        Some(negative)
      } else {
        Some(positive)
      }
    }
  }
}
