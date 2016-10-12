package com.lynbrookrobotics.potassium.tasks

abstract class ContinuousTask extends Task {
  def onStart(): Unit
  def onEnd(): Unit

  override def init(): Unit = onStart()
  override def abort(): Unit = onEnd()
}
