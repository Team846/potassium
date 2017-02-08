package com.lynbrookrobotics.potassium

import org.scalatest.FunSuite
import com.lynbrookrobotics.potassium.units.GenericValue._
import squants.space.Radians
import squants.Seconds

class GenericValuesTest extends FunSuite {
  test("Can multiply generic value by time to get generic integral") {
    val genericValue = Radians(1).toGeneric

    assert((genericValue * Seconds(2)).toString == "2.0 rad * s")
  }

  test("Can divide generic value by time to get generic derivative") {
    val genericValue = Radians(2).toGeneric

    assert((genericValue / Seconds(2)).toString == "1.0 rad / s")
  }

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
