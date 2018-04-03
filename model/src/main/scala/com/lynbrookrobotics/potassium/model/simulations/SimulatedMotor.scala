package com.lynbrookrobotics.potassium.model.simulations

import com.lynbrookrobotics.potassium.clock.Clock
import com.lynbrookrobotics.potassium.streams._
import squants.time.Time
import squants.{Dimensionless, Percent}

class SimulatedMotor(rootStream: Stream[Unit]) {
  val initialOutput = Percent(0)

  private var lastOutput = initialOutput
  val outputStream = rootStream.map(_ => lastOutput)

  private def capAt100Percent(input: Dimensionless): Dimensionless = {
    input min Percent(100) max Percent(-100)
  }

  def set(toSet: Dimensionless): Unit = {
    lastOutput = capAt100Percent(toSet)
  }
}