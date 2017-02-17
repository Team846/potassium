package com.lynbrookrobotics.potassium.sensors.imu

import org.scalatest.FunSuite
import squants.motion.{AngularVelocity, DegreesPerSecond}

class Value3DTest extends FunSuite {
  test("Tests functions of Value3D") {
    val value1: Value3D[AngularVelocity] = Value3D[AngularVelocity](DegreesPerSecond(1), DegreesPerSecond(2), DegreesPerSecond(3))
    val value2: Value3D[AngularVelocity] = Value3D[AngularVelocity](DegreesPerSecond(3), DegreesPerSecond(2), DegreesPerSecond(1))

    assertResult(Value3D[AngularVelocity](DegreesPerSecond(4),
      DegreesPerSecond(4),
      DegreesPerSecond(4))) (value1 + value2)
    assertResult(Value3D[AngularVelocity](DegreesPerSecond(1), DegreesPerSecond(2), DegreesPerSecond(3))) (value1.*(1D))
  }
}
