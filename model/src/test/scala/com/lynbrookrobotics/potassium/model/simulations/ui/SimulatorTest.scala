package com.lynbrookrobotics.potassium.model.simulations.ui

import org.scalatest.FunSuite
import squants.space.Feet

class SimulatorTest extends FunSuite {
  test("Check that the simulator GUI starts up, and can change the canvas") {
    val simulatorGUI = new SimulatorGUI(Feet(10), Feet(10))
    simulatorGUI.startGUI()
  }
}
