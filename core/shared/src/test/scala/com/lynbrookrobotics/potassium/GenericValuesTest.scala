package com.lynbrookrobotics.potassium

import org.scalatest.FunSuite

class GenericValuesTest extends FunSuite {
  test("Can compile integral of unit with no squants integral") {

    assertCompiles(
      """
        |import com.lynbrookrobotics.potassium.units.GenericValue.toGenericValue
        |val signal = Signal(squants.space.Degrees(0)).toPeriodic.integral
      """.stripMargin)
  }

  test("Can compile integral of unit with squants integral") {

    assertCompiles(
      """
        |import com.lynbrookrobotics.potassium.units.GenericValue.toGenericValue
        |val signal = Signal(squants.motion.DegreesPerSecond(0)).toPeriodic.integral
      """.stripMargin)
  }
}
