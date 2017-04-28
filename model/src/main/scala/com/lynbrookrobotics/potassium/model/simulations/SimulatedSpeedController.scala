package com.lynbrookrobotics.potassium.model.simulations

import com.lynbrookrobotics.potassium.Signal
import squants.{Acceleration, Dimensionless, Percent}

class SimulatedSpeedController {
  var lastOutput = Percent(0)
  val outputSignal = Signal(lastOutput).toPeriodic

  private def capAt100Percent(input: Dimensionless) = {
    input min Percent(100) max Percent(-100)
  }

  def set(toSet: Dimensionless): Unit = {
    lastOutput = capAt100Percent(toSet)
  }
}

