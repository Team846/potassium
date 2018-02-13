package com.lynbrookrobotics.potassium.model.simulations.ui

import java.io.File
import java.util.{Timer, TimerTask}

import com.lynbrookrobotics.potassium.units.Point
import squants.space.{Degrees, Feet}


object SimulatorTest extends App {
  val simulatorGUI = new SimulatorGUI(Feet(10), Feet(10))
  simulatorGUI.startGUI()
  simulatorGUI.useLogFile(new File("simlog-,(0.0 ft,0.0 ft,0.0 ft),(-0.1 ft,5.0 ft,0.0 ft),(-5.0 ft,10.0 ft,0.0 ft)"))
  val timer = new Timer()
  timer.schedule(new TimerTask {
    override def run(): Unit = {

    }
  }, 0, 1000)
}
