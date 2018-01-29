package com.lynbrookrobotics.potassium

import org.scalatest.FunSuite
import squants.Each
import squants.space.{Degrees, Meters, Radians}
import units._

class RatioTest extends FunSuite {
  test("Ratio multiply produces correct values") {
    val ratio = Meters(5) / Degrees(10)
    assert((ratio * Degrees(20)).toMeters == 10)
    assert((ratio * ratio.recip).value == 1)
  }

  test("Ratio toString produces correct values") {
    val ratio = Meters(5) / Radians(10)
    assert(ratio.toString == "5.0 m / 10.0 rad")
  }

  test("Ratio recip produces correct values") {
    val ratio = Meters(5) / Radians(10)
    assert(ratio == ratio.recip.recip)
    assert(ratio.recip.recip.recip.recip == ratio.recip.recip)
    assert(ratio.recip.recip.recip == ratio.recip)
  }
}
