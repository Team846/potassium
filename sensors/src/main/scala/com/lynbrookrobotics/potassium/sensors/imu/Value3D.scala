package com.lynbrookrobotics.potassium.sensors.imu

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
  def +(toAdd: Value3D[Q]): Value3D[Q] = {
    Value3D(
      x + toAdd.x,
      y + toAdd.y,
      z + toAdd.z
    )
  }

  /**
    * Subtracts this 3D value from another one.
    * @param toAdd the 3D value to subtract
    * @return the combined 3D value
    */
  def -(toAdd: Value3D[Q]): Value3D[Q] = {
    Value3D(
      x - toAdd.x,
      y - toAdd.y,
      z - toAdd.z
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
}
