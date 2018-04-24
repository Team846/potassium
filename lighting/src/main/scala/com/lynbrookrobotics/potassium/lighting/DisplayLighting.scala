package com.lynbrookrobotics.potassium.lighting

import com.lynbrookrobotics.potassium.Component
import com.lynbrookrobotics.potassium.streams.Stream
import com.lynbrookrobotics.potassium.tasks.ContinuousTask

/**
  * Continuous Task that you feed a communicator and an rgb value.
  * Defaults to black onEnd
  */
class DisplayLighting(signal: Stream[Int], lightingComponent: LightingComponent) extends ContinuousTask {
  override def onStart(): Unit = {
    lightingComponent.setController(signal)
  }

  override def onEnd(): Unit = {
    lightingComponent.resetToDefault()
  }

  override val dependencies: Set[Component[_]] = Set(lightingComponent)
}
