package com.lynbrookrobotics.potassium.units

import squants.Quantity

/**
 * Constructs a new 3D value given X, Y, and Z axes.
 */
case class Value3D[Q <: Quantity[Q]](x: Q, y: Q, z: Q) {

  def this(value3D: Value3D[Q]) = {
    this(value3D.x, value3D.y, value3D.z)
  }

  /**
   * Adds this 3D value to another one.
   * @param toAdd the 3D value to add
   * @return the combined 3D value
   */
  def +(toAdd: Value3D[Q]): Value3D[Q] = {
    Value3D(
      x + toAdd.x,
      y + toAdd.y,
      z + toAdd.z
    )
  }

  /**
   * Subtracts this 3D value from another one.
   * @param toSub the 3D value to subtract
   * @return the combined 3D value
   */
  def -(toSub: Value3D[Q]): Value3D[Q] = {
    Value3D(
      x - toSub.x,
      y - toSub.y,
      z - toSub.z
    )
  }

  /**
   * Multiplies this 3D value by a scalar.
   * @param scalar the value to multiply the axes by
   * @return the scaled 3D value
   */
  def times(scalar: Double): Value3D[Q] = {
    Value3D(
      scalar * x,
      scalar * y,
      scalar * z
    )
  }

  def *(scaler: Double): Value3D[Q] = this.times(scaler)
}

case class Matrix3by3(elems: Seq[Double]*) {
  if (elems.length != 3 || elems.forall(_.length != 3)) {
    throw new IllegalArgumentException("matrix must be 3 by 3")
  }

  def *[T <: Quantity[T]](other: Value3D[T]): Value3D[T] = {
    Value3D(
      elems(0)(0) * other.x + elems(0)(1) * other.y + elems(0)(2) * other.z,
      elems(1)(0) * other.x + elems(1)(1) * other.y + elems(1)(2) * other.z,
      elems(2)(0) * other.x + elems(2)(1) * other.y + elems(2)(2) * other.z
    )
  }
}
