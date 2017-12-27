package com.lynbrookrobotics.potassium

import com.lynbrookrobotics.potassium.units.Value3D
import org.scalatest.FunSuite
import squants.Length
import squants.space.Meters

class Value3DTest extends FunSuite {
  test("Matrix multiplication functions correctly") {
    val original : Value3D[Length] = Value3D(Meters(1.0),Meters(1.0),Meters(1.0))
    val matrix = ((1.0,1.0,1.0),(1.0,1.0,1.0),(1.0,1.0,1.0))

    val newVal : Value3D[Length] = original * matrix

    val testVal: Value3D[Length] = Value3D(Meters(3.0), Meters(3.0), Meters(3.0))

    assert(newVal == testVal)
  }
}
