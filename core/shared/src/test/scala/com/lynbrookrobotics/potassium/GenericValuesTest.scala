package com.lynbrookrobotics.potassium

import org.scalatest.FunSuite

import com.lynbrookrobotics.potassium.units.GenericValue._
import com.lynbrookrobotics.potassium.streams.Stream

import squants.space.{Angle, Radians}
import squants.motion.AngularVelocity
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

  test("Can compile integral of dimension with no squants integral") {
    assertCompiles(
      """
        |import com.lynbrookrobotics.potassium.units.GenericValue.toGenericValue
        |val signal = Stream.manual[Angle]._1.integral
      """.stripMargin)
  }

  test("Can compile integral of dimension with squants integral") {
    assertCompiles(
      """
        |import com.lynbrookrobotics.potassium.units.GenericValue.toGenericValue
        |val signal = Stream.manual[AngularVelocity]._1.integral
      """.stripMargin)
  }
}
