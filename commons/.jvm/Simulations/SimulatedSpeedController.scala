package com.lynbrookrobotics.potassium.commons.drivetrain.Simulations

import com.lynbrookrobotics.potassium.Signal
import squants.{Acceleration, Dimensionless, Percent}

class SimulatedSpeedController {
  var lastOutput = Percent(0)
  val outputSignal = Signal(lastOutput).toPeriodic

  private def capAt100Percent(toCap: Dimensionless): Dimensionless = {
    toCap min Percent(100) max -Percent(100)
  }

  def set(toSet: Dimensionless): Unit = {
    lastOutput = capAt100Percent(toSet)
  }
}
