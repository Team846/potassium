package com.lynbrookrobotics.potassium

import com.lynbrookrobotics.potassium.units.Line
import org.scalatest.FunSuite
import squants.space.{Degrees, Inches}

class LineTest extends FunSuite {
  test("check where slope = 0") {
    val line = Line(Degrees(0), Inches(0))
    assert(line.xIntercept == Inches(Double.PositiveInfinity))
  }
  test("check that xIntercept is correct") {
    val line = Line(Degrees(45), Inches(1))
    // Timothy wrote this rounding part
    assert((line.xIntercept.toInches - 0.5).toInt == Inches(-1).toInches.toInt)
  }
  test("check that xIntercept is correct (2)") {
    val line = Line(Degrees(135), Inches(1))
    assert((line.xIntercept.toInches + 0.5).toInt == Inches(1).toInches.toInt)
  }

  test("negative radius for BusDriving") {
    val line = Line(Degrees(45), Inches(1))
    assert(line.xIntercept.toInches < 0)
  }
}
