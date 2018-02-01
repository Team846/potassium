package com.lynbrookrobotics.potassium.model.simulations.ui

import java.io.File

import com.lynbrookrobotics.potassium.units.Point
import squants.space.{Degrees, Feet}


object SimulatorTest extends App {
  val simulatorGUI = new SimulatorGUI(Feet(10), Feet(10))
//  simulatorGUI.startGUI()
//  simulatorGUI.update(Point(Feet(1), Feet(1)), Degrees(45))
  simulatorGUI.useLogFile(new File("simlog-,(0.0 ft,0.0 ft,0.0 ft),(-0.1 ft,5.0 ft,0.0 ft),(-5.0 ft,10.0 ft,0.0 ft)"))
}
