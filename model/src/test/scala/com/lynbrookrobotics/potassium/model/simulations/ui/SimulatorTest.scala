package com.lynbrookrobotics.potassium.model.simulations.ui

import com.lynbrookrobotics.potassium.units.Point
import org.scalatest.FunSuite
import squants.space.{Degrees, Feet}

class SimulatorTest extends FunSuite {
  test("Test GUI") {
    val simulatorGUI = new SimulatorGUI(Feet(10), Feet(10))
    simulatorGUI.startGUI()
    simulatorGUI.update(Point(Feet(1), Feet(1)), Degrees(45))
  }
}
