package com.lynbrookrobotics.potassium.commons.drivetrain

import com.lynbrookrobotics.potassium.commons.drivetrain.unicycle.control.purePursuit.MathUtilities
import org.scalatest.FunSuite
import com.lynbrookrobotics.potassium.units.{Point, Segment}
import squants.space.Feet

class MathUtilitiesTest extends FunSuite {
  val origin = Point.origin

  test("Test circle intersection with vertical line") {
    val firstPoint = new Point(Feet(0), Feet(-2))
    val secondPoint = new Point(Feet(0), Feet(2))

    val segment = Segment(firstPoint, secondPoint)

    val solutions = segment.intersectionWithCircle(
      origin,
      Feet(1)
    )
    val posSolution = new Point(Feet(0), Feet(1))
    val negSolution = new Point(Feet(0), Feet(-1))

    assert(solutions.isDefined)
    assert(solutions.get._1 == negSolution)
    assert(solutions.get._2 == posSolution)

    val solutionClosestToEnd = MathUtilities.intersectionLineCircleFurthestFromStart(
      segment,
      origin,
      Feet(1)
    )

    assert(solutionClosestToEnd.isDefined)
    assert(solutionClosestToEnd.get == posSolution)
  }

  test("Test circle intersection with diagonal line") {
    val firstPoint = new Point(Feet(-2), Feet(-2))
    val secondPoint = new Point(Feet(2), Feet(2))

    val segment = Segment(firstPoint, secondPoint)

    val solutions = segment.intersectionWithCircle(
      origin,
      Feet(1)
    )
    val posSolution = new Point(Feet(Math.sqrt(2) / 2), Feet(Math.sqrt(2) / 2))
    val negSolution = new Point(Feet(-Math.sqrt(2) / 2), Feet(-Math.sqrt(2) / 2))

    assert(solutions.isDefined)
    assert(solutions.get._1 == negSolution)
    assert(solutions.get._2 == posSolution)
  }

  test("Circle and non intersecting segment do not intersect") {
    val secondPoint = new Point(Feet(1), Feet(0))

    assert(MathUtilities.intersectionLineCircleFurthestFromStart(
      Segment(origin, secondPoint),
      new Point(Feet(0), Feet(1)),
      Feet(0.5)
    ).isEmpty, "Found non existent intersection!")
  }

  test("find intersection segment from (0, 0) to (0, 2) with circle radius 1 with center (0, 1)"){
    val segment = Segment(
      origin,
      new Point(Feet(2), Feet(0))
    )
    val currPose = new Point(Feet(0), Feet(1))

    val solutionClosestToEnd = MathUtilities.intersectionLineCircleFurthestFromStart(
      segment,
      currPose,
      Feet(2)
    )

    implicit val tolerance = Feet(0.01)

    assert(solutionClosestToEnd.isDefined)
    assert(solutionClosestToEnd.get.y ~= Feet(0))
    assert(solutionClosestToEnd.get.x ~= Feet(1.732))
  }
}
