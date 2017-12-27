package com.lynbrookrobotics.potassium

import com.lynbrookrobotics.potassium.units.Point
import org.scalatest.FunSuite
import squants.space.{Angle, Degrees, Feet}

class RotationTest extends FunSuite{
  test("Rotation of point by 90 degrees results in correct point")
  {
    val originalPoint: Point = Point(Feet(1),Feet(1),Feet(0))
    val angleToRotate: Angle = Degrees(90)
    val newPoint: Point = originalPoint.rotateBy(angleToRotate)
    val testPoint: Point = Point(Feet(-1), Feet(1), Feet(0))
    implicit val tolerance = Feet(0.0001)
    assert(newPoint ~= testPoint)
  }

  test("Rotation of point by -90 degrees results in correct point")
  {
    val originalPoint: Point = Point(Feet(1),Feet(1),Feet(0))
    val angleToRotate: Angle = Degrees(-90)
    val newPoint: Point = originalPoint.rotateBy(angleToRotate)
    val testPoint: Point = Point(Feet(1), Feet(-1), Feet(0))
    implicit val tolerance = Feet(0.0001)
    assert(newPoint ~= testPoint)
  }

  test("Rotation of point at origin by any angle results in no displacement")
  {
    val originalPoint: Point = Point(Feet(0), Feet(0), Feet(0))
    val angleToRotate: Angle = Degrees(47)
    val newPoint: Point = originalPoint.rotateBy(angleToRotate)
    val testPoint: Point = Point(Feet(0), Feet(0), Feet(0))
    assert(newPoint == testPoint)
  }

  test("Rotation about another point by 90 degrees results in correct point")
  {
    val originalPoint: Point = Point(Feet(0), Feet(0), Feet(0))
    val center: Point = Point(Feet(1), Feet(0), Feet(0))
    val angleToRotate: Angle = Degrees(90)
    val newPoint: Point = originalPoint.rotateAround(center, angleToRotate)
    val testPoint: Point = Point(Feet(1), Feet(-1), Feet(0))
    implicit val tolerance = Feet(0.0001)
    assert(newPoint ~= testPoint)
  }

  test("Rotation about another point by -90 degrees results in correct point")
  {
    val originalPoint: Point = Point(Feet(0), Feet(0), Feet(0))
    val center: Point = Point(Feet(1), Feet(0), Feet(0))
    val angleToRotate: Angle = Degrees(-90)
    val newPoint: Point = originalPoint.rotateAround(center, angleToRotate)
    val testPoint: Point = Point(Feet(1), Feet(1), Feet(0))
    implicit val tolerance = Feet(0.0001)
    assert(newPoint ~= testPoint)
  }

  test("Rotation about origin by 90 degrees results in correct point")
  {
    val originalPoint: Point = Point(Feet(1), Feet(1), Feet(0))
    val center: Point = Point(Feet(0), Feet(0), Feet(0))
    val angleToRotate: Angle = Degrees(90)
    val newPoint: Point = originalPoint.rotateAround(center, angleToRotate)
    val testPoint: Point = Point(Feet(-1), Feet(1), Feet(0))
    implicit val tolerance = Feet(0.0001)
    assert(newPoint ~= testPoint)
  }
}
