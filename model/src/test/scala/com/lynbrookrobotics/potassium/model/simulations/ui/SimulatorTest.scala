package com.lynbrookrobotics.potassium.model.simulations.ui

import com.lynbrookrobotics.potassium.clock.JavaClock
import com.lynbrookrobotics.potassium.units.Point
import org.scalatest.FunSuite
import squants.space.{Degrees, Feet}
import squants.time.Seconds


class SimulatorTest extends FunSuite {
  test("Test GUI") {
    val simulatorGUI = new SimulatorGUI(Feet(10), Feet(10))
    simulatorGUI.startGUI()
    simulatorGUI.update(Point(Feet(1), Feet(1)), Degrees(45))

    JavaClock.apply(Seconds(10)) { _ =>
      simulatorGUI.update(Point(Feet(1), Feet(1)), Degrees(System.currentTimeMillis() % 360))

      val startTime = JavaClock.currentTime
      while (JavaClock.currentTime - startTime <= Seconds(10)) {

        // nothing
      }
    }
  }
}











