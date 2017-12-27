package com.lynbrookrobotics.potassium

import com.lynbrookrobotics.potassium.units.Value3D
import org.scalatest.FunSuite

class Value3DTest extends FunSuite {
  test("Matrix multiplication functions correctly") {
    val original : Value3D[Double] = Value3D(1.0,1.0,0.0)
    val matrix = ((1.0,1.0,1.0),(1.0,1.0,1.0),(1.0,1.0,1.0))

    val newVal : Value3D[Double] = original * matrix

    val testVal: Value3D[Double] = Value3D(3.0, 3.0, 3.0)

    assert(newVal == testVal)
  }
}
