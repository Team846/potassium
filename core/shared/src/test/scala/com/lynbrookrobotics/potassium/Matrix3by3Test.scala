package com.lynbrookrobotics.potassium

import com.lynbrookrobotics.potassium.units.{Matrix3by3, Value3D}
import org.scalatest.FunSuite
import squants.space.Meters

class Matrix3by3Test extends FunSuite {
  test("Matrix multiplication functions correctly") {
    val original = Value3D(Meters(1.0), Meters(1.0), Meters(1.0))
    val matrix = Matrix3by3(
      Seq(1.0, 1.0, 1.0),
      Seq(1.0, 1.0, 1.0),
      Seq(1.0, 1.0, 1.0))

    val newVal = matrix * original

    val testVal = Value3D(Meters(3.0), Meters(3.0), Meters(3.0))

    assert(newVal == testVal)
  }
}
