package com.lynbrookrobotics.potassium.commons.drivetrain

import squants.Quantity

/**
  * Constructs a new 3D value given X, Y, and Z axes.
  */
case class Value3D[Q <: Quantity[Q]](x: Q, y: Q, z: Q) {
  /**
    * Adds this 3D value to another one.
    * @param toAdd the 3D value to add
    * @return the combined 3D value
    */
  def + (toAdd: Value3D[Q]): Value3D[Q] = {
    Value3D(
      x + toAdd.x,
      y + toAdd.y,
      z + toAdd.z
    )
  }

  def - (toSub: Value3D[Q]): Value3D[Q] = {
    Value3D(
      x - toSub.x,
      y - toSub.y,
      z - toSub.z
    )
  }

  def min(other: Value3D[Q]): Value3D[Q] = {
    val magnitudeOther =
      other.x.value * other.x.value +
      other.y.value * other.y.value +
      other.z.value * other.z.value
    val magnitudeThis =
      x.value * x.value +
      y.value * y.value +
      z.value * z.value

    if (magnitudeOther >= magnitudeThis) other
    else this
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
}
