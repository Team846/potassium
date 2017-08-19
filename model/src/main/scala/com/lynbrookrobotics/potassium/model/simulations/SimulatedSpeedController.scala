package com.lynbrookrobotics.potassium.model.simulations

import com.lynbrookrobotics.potassium.streams._
import squants.{Dimensionless, Percent}

class SimulatedSpeedController {
  var lastOutput = Percent(0)

  private val (manualStream, publish) = Stream.manual[Dimensionless]
  val outputStream = manualStream.map(_ => lastOutput)

  private def capAt100Percent(input: Dimensionless) = {
    input min Percent(100) max Percent(-100)
  }

  def publishLastOutput(): Unit = {
    publish.apply(lastOutput)
  }

  def set(toSet: Dimensionless): Unit = {
    lastOutput = capAt100Percent(toSet)
  }
}