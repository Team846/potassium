package com.lynbrookrobotics.potassium

import com.lynbrookrobotics.potassium.units.Point
import org.scalatest.FunSuite
import squants.space.{Degrees, Feet}

class RotationTest extends FunSuite{
  test("Rotation of point by 90 degrees results in correct point") {
    val calculated = Point(Feet(1), Feet(1)).rotateBy(Degrees(90))

    implicit val tolerance = Feet(0.0001)
    assert(calculated ~= Point(Feet(-1), Feet(1)))
  }

  test("Rotation of point by -90 degrees results in correct point") {
    val calculated = Point(Feet(1), Feet(1)).rotateBy(Degrees(-90))

    implicit val tolerance = Feet(0.0001)
    assert(calculated ~= Point(Feet(1), Feet(-1)), s"was $calculated")
  }

  test("Rotation of point at origin by any angle results in no displacement") {
    val calculated = Point.origin.rotateBy(Degrees(45))
    assert(calculated == Point.origin)
  }

  test("Rotation about another point by 90 degrees results in correct point") {
    val calculated = Point.origin.rotateAround(
      center = Point(Feet(1), Feet(0)),
      Degrees(90))

    implicit val tolerance = Feet(0.0001)
    assert(calculated ~= Point(Feet(1), Feet(-1)))
  }

  test("Rotation about another point by -90 degrees results in correct point") {
    val calculated = Point.origin.rotateAround(
      center = Point(Feet(1), Feet(0)),
      Degrees(-90))

    implicit val tolerance = Feet(0.0001)
    assert(calculated ~= Point(Feet(1), Feet(1)))
  }

  test("Rotation about origin by 90 degrees results in correct point") {
    val newPoint: Point = Point(Feet(1), Feet(1)).rotateAround(
      center = Point.origin,
      Degrees(90))

    implicit val tolerance = Feet(0.0001)
    assert(newPoint ~= Point(Feet(-1), Feet(1)))
  }
}
