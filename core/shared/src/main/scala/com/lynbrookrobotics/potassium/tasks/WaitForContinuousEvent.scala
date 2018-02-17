package com.lynbrookrobotics.potassium.tasks

import com.lynbrookrobotics.potassium.events.ContinuousEvent

class WaitForContinuousEvent(e: ContinuousEvent) extends FiniteTask{
  e.foreach(() => finished())

  override protected def onStart(): Unit = {}

  override protected def onEnd(): Unit = {}
}
