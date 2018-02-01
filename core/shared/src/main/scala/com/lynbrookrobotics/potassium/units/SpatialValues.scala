package com.lynbrookrobotics.potassium.units

import squants.space._

class Point(override val x: Length,
            override val y: Length,
            override val z: Length = Feet(0)) extends Value3D(x, y, z) {
  def this(value3D: Value3D[Length]) = this(value3D.x, value3D.y, value3D.z)

  def distanceTo(other: Point): Length = Segment(this, other).length

  def dot(other: Point): Area = {
    this.x * other.x + this.y * other.y + this.z * other.z
  }

  def magnitude: Length = (x * x + y * y + z * z).squareRoot

  def -(other: Point): Point = new Point(super.-(other))
  def +(other: Point): Point = new Point(super.+(other))
  override def *(scalar: Double): Point = new Point(super.*(scalar))

  def ~=(other: Point)(implicit tolerance: Length): Boolean = {
    (other.x ~= this.x) &&
      (other.y ~= this.y) &&
      (other.z ~= this.z)
  }

  /**
    * produce a point rotated about the z axis with the given
    * angle of rotation
    * @param angle the angle to rotate by
    * @return a new point rotated about the z axis with given angle
    */
  def rotateBy(angle: Angle): Point = {
    val rotationMatrix =
      Matrix3by3(
        Seq(angle.cos, -angle.sin, 0.0),
        Seq(angle.sin,  angle.cos, 0.0),
        Seq(0.0,        0.0,       1.0)
      )

    Point(rotationMatrix * this)
  }

  def rotateAround(center: Point, angle: Angle): Point = {
    val translatedPoint = this - center
    val rotatedPoint = translatedPoint.rotateBy(angle)
    rotatedPoint + center
  }

  def onLine(toTest: Segment, tolerance: Length): Boolean = {
    implicit val implicitTolerance = tolerance

    val xySlope = toTest.xySlope
    if (xySlope != Double.NaN && math.abs(xySlope) != Double.PositiveInfinity) {
      // Uses point slope form of line to determine if the line constructed from
      // start and end contains the given point
      ((y - toTest.start.y) - xySlope * (x - toTest.start.x)).abs <= tolerance
    } else {
      // If the segment is directly upwards, slope is Nan or Infinity
      x ~= toTest.start.x
    }
  }
}

object Point {
  def origin: Point = new Point(Feet(0), Feet(0))

  def apply(x: Length, y: Length, z: Length = Feet(0)): Point = {
    new Point(x, y, z)
  }

  def apply(v: Value3D[Length]): Point = {
    new Point(v.x, v.y, v.z)
  }
}

case class Segment(start: Point, end: Point) {
  val diff = end - start
  val length = diff.magnitude
  val xySlope = (end.y - start.y) / (end.x - start.x)

  val dz = end.z - start.z
  val dy = end.y - start.y
  val dx = end.x - start.x

  def angle: Angle = {
    Radians(math.atan2(dy.toFeet, dx.toFeet))
  }

  def pointClosestToOnLine(pt: Point): Point = {
    val lengthSquared = length.squared
    val apDiff = pt - start
    val interpolation = diff.dot(apDiff) / lengthSquared
    start + ((end - start) * interpolation)
  }
}