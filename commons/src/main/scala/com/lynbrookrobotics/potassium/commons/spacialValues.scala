package com.lynbrookrobotics.potassium.commons

import com.lynbrookrobotics.potassium.Value3D
import squants.space.{Area, Feet, Length}

class Point(
    override val x: Length,
    override val y: Length,
    override val z: Length) extends Value3D(x, y, z) {
  def this(value3D: Value3D[Length]) = this(value3D.x, value3D.y, value3D.z)
  def this(x: Length, y: Length) = this(x, y, Feet(0))

  def distanceTo(other: Point): Length = Segment(this, other).length

  def dot(other: Point): Area = {
    this.x * other.x + this.y * other.y + this.z * other.z
  }

  def magnitude: Length = (x * x + y * y + z * z).squareRoot

  def zip(other: Point) = Segment(this, other)

  // Better solution, perhaps using type aliases?
  def - (other: Point) = new Point(super.-(other))
  def + (other: Point) = new Point(super.+(other))
  override def * (scalar: Double) = new Point(super.*(scalar))
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
  def containsInXY(toTest: Point): Boolean = {
    val withinBoundaries =
      toTest.x <= end.x &&
      toTest.y <= end.y &&
      toTest.x >= start.x &&
      toTest.y >= start.y

    // Uses point slope form of line to determine if the line constructed from
    // start and end contains the given point
    val onLine = (end.y - toTest.y) == xySlope * (end.x - toTest.x)
    withinBoundaries && onLine
  }
}