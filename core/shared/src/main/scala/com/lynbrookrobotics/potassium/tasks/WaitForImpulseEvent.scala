package com.lynbrookrobotics.potassium.tasks

import com.lynbrookrobotics.potassium.events.ImpulseEvent

class WaitForImpulseEvent(impulseEvent: ImpulseEvent) extends FiniteTask {
  impulseEvent.foreach(() => finished())

  override protected def onStart(): Unit = {}

  override protected def onEnd(): Unit = {}
}
