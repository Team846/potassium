package com.lynbrookrobotics.potassium.lighting

import java.awt.Color

import com.lynbrookrobotics.potassium.Signal
import com.lynbrookrobotics.potassium.tasks.ContinuousTask

/**
  * Continuous Task that you feed a communicator and an rgb value.
  * Defaults to black onEnd
  */
class DisplayLighting(signal: Signal[Int], lightingComponent: LightingComponent) extends ContinuousTask {
  override def onStart(): Unit = {
    lightingComponent.setController(signal.toPeriodic)
  }

  override def onEnd(): Unit = {
    lightingComponent.resetToDefault()
  }
}
