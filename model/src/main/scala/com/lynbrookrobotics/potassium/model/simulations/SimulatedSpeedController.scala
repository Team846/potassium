package com.lynbrookrobotics.potassium.model.simulations

import com.lynbrookrobotics.potassium.streams._
import squants.{Dimensionless, Percent}

class SimulatedSpeedController {
  var lastOutput = Percent(0)

  val (manualStream, publish) = Stream.manual
  val outputStream = manualStream.map(_ => lastOutput)

  private def capAt100Percent(input: Dimensionless) = {
    input min Percent(100) max Percent(-100)
  }

  def set(toSet: Dimensionless): Unit = {
    lastOutput = capAt100Percent(toSet)
  }
}