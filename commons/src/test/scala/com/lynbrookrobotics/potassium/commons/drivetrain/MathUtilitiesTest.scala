import org.scalatest.FunSuite
import com.lynbrookrobotics.potassium.commons.drivetrain.MathUtilities
import com.lynbrookrobotics.potassium.units.{Point, Segment}
import squants.space.Feet

class MathUtilitiesTest extends FunSuite {
  val origin = new Point(Feet(0), Feet(0))

  test("Test circle intersection with vertical line") {
    val firstPoint = new Point(Feet(0), Feet(-2))
    val secondPoint = new Point(Feet(0), Feet(2))

    val segment = Segment(firstPoint, secondPoint)

    val solutions = MathUtilities.interSectionCircleLine(
      segment,
      origin,
      Feet(1)
    )
    val posSolution = new Point(Feet(0), Feet(1))
    val negSolution = new Point(Feet(0), Feet(-1))

    assert(solutions.isDefined, "Solutions don't exist!")
    assert(solutions.get._1 == negSolution, "First solution is: " + solutions.get._1)
    assert(solutions.get._2 == posSolution, "Second solution is: " + solutions.get._2)

    val solutionClosestToEnd = MathUtilities.intersectionClosestToEnd(
      segment,
      origin,
      Feet(1)
    )

    assert(solutionClosestToEnd.isDefined, "Solution doesn't exist!")
    assert(solutionClosestToEnd.get == posSolution, "Solution is: " + solutionClosestToEnd)
  }

  test("Test circle intersection with diagnol line") {
    val firstPoint = new Point(Feet(-2), Feet(-2))
    val secondPoint = new Point(Feet(2), Feet(2))

    val segment = Segment(firstPoint, secondPoint)

    val solutions = MathUtilities.interSectionCircleLine(
      segment,
      origin,
      Feet(1)
    )
    val posSolution = new Point(Feet(Math.sqrt(2) / 2), Feet(Math.sqrt(2) / 2))
    val negSolution = new Point(Feet(-Math.sqrt(2) / 2), Feet(-Math.sqrt(2) / 2))

    assert(solutions.isDefined, "Solutions don't exist!")
    assert(solutions.get._1 == negSolution, "First solution is: " + solutions.get._1)
    assert(solutions.get._2 == posSolution, "Second solution is: " + solutions.get._2 )
  }

  test("Circle and non intersecting segment do not intersect") {
    val secondPoint = new Point(Feet(1), Feet(0))

    assert(MathUtilities.interSectionCircleLine(
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

    val solutionClosestToEnd = MathUtilities.intersectionClosestToEnd(
      segment,
      currPose,
      Feet(2)
    )

//    val segment = Segment(
//      origin,
//      new Point(Feet(2), Feet(0))
//    )
//    val currPose = new Point(Feet(0.01), Feet(1))
//
//    val solutionClosestToEnd = MathUtilities.intersectionClosestToEnd(
//      segment,
//      currPose,
//      Feet(11000)
//    )

    implicit val tolerance = Feet(0.01)
    assert(solutionClosestToEnd.isDefined, "No solutions found!")
    println(s" solution $solutionClosestToEnd")
    assert(solutionClosestToEnd.get.y ~= Feet(0), "Solutions are not equal!")
    assert(solutionClosestToEnd.get.x ~= Feet(1.732))
  }
}