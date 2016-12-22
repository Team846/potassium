package com.lynbrookrobotics.potassium.tasks

/**
  * Represents a task that can only be stopped by an external impulse.
  */
abstract class ContinuousTask extends Task {
  def onStart(): Unit
  def onEnd(): Unit

  /**
    * Starts the continuous task.
    */
  override def init(): Unit = onStart()

  /**
    * Stops the continuous task.
    */
  override def abort(): Unit = onEnd()
}
