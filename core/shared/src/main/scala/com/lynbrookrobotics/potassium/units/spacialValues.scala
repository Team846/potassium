package com.lynbrookrobotics.potassium.units

import squants.space.{Area, Feet, Length}

class Point(override val x: Length,
            override val y: Length,
            override val z: Length) extends Value3D(x, y, z) {
  def this(value3D: Value3D[Length]) = this(value3D.x, value3D.y, value3D.z)
  def this(x: Length, y: Length) = this(x, y, Feet(0))

  def distanceTo(other: Point): Length = Segment(this, other).length

  def dot(other: Point): Area = {
    this.x * other.x + this.y * other.y + this.z * other.z
  }

  def magnitude: Length = (x * x + y * y + z * z).squareRoot

  def - (other: Point) = new Point(super.-(other))
  def + (other: Point) = new Point(super.+(other))
  override def * (scalar: Double) = new Point(super.*(scalar))

  def ~=(other: Point)(implicit tolerance: Length): Boolean = {
    (other.x ~= this.x) &&
      (other.y ~= this.y) &&
      (other.z ~= this.z)
  }
}

object Point {
  def origin: Point = new Point(Feet(0), Feet(0))
}

case class Segment(start: Point, end: Point) {
  val diff = end - start
  val length = diff.magnitude
  val xySlope = (end.y - start.y) / (end.x - start.x)

  val dz = end.z - start.z
  val dy = end.y - start.y
  val dx = end.x - start.x

  /**
    *
    * @param toTest to the point to test if contained by this
    * @return whether the given point is contained by this segment IN THE XY
    *         plane
    */
  def containsInXY(toTest: Point, tolerance: Length): Boolean = {
    val minX = (start.x min end.x) - Feet(0.01)
    val maxX = (start.x max end.x) + Feet(0.01)

    val minY = (start.y min end.y) - Feet(0.01)
    val maxY = (start.y max end.y) + Feet(0.01)

    val withinBoundaries =
      toTest.x >= minX && toTest.x <= maxX &&
        toTest.y >= minY && toTest.y <= maxY


    // If the segment is directly upwards, slope is Nan or Infinity
    if (xySlope != Double.NaN && Math.abs(xySlope) != Double.PositiveInfinity) {
      // Uses point slope form of line to determine if the line constructed from
      // start and end contains the given point
      val onLine = ((toTest.y - start.y) - xySlope * (toTest.x - start.x)).abs <= tolerance

      withinBoundaries && onLine
    } else {
      implicit val implicitTolerance = tolerance
      withinBoundaries && (toTest.x ~= start.x)
    }
  }
}