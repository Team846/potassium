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

  def segmentTo(other: Point) = Segment(this, other)

  def - (other: Point) = new Point(super.-(other))
  def + (other: Point) = new Point(super.+(other))
  override def * (scalar: Double) = new Point(super.*(scalar))
  def == (other: Point): Boolean = {
    ==(other, Feet(0.5))
  }

  def == (other: Point, tolerance: Length): Boolean = {
    (other.x - this.x).abs <= tolerance &&
      (other.y - this.y).abs <= tolerance &&
      (other.z - this.z).abs <= tolerance
  }
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
    val withinBoundaries =
      toTest.x <= end.x &&
        toTest.y <= end.y &&
        toTest.x >= start.x &&
        toTest.y >= start.y

    // Uses point slope form of line to determine if the line constructed from
    // start and end contains the given point
    val onLine = ((end.y - toTest.y) - xySlope * (end.x - toTest.x)).abs <= tolerance
    withinBoundaries && onLine
  }
}