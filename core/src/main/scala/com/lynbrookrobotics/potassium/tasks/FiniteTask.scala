package com.lynbrookrobotics.potassium.tasks

import com.lynbrookrobotics.potassium.clock.Clock
import squants.Time

import scala.ref.WeakReference

trait FiniteTaskFinishedListener {
  def onFinished(task: FiniteTask): Unit
}

abstract class FiniteTask extends Task {
  private var running = false
  private var listeners: List[WeakReference[FiniteTaskFinishedListener]] = List.empty

  def addFinishedListener(listener: FiniteTaskFinishedListener): Unit = {
    listeners = WeakReference(listener) :: listeners
  }

  protected def finished(): Unit = {
    if (running) {
      onEnd()
      running = false

      listeners = listeners.filter { ref =>
        ref.get match {
          case Some(listener) =>
            listener.onFinished(this)
            true

          case None => false
        }
      }
    }
  }

  def onStart(): Unit
  def onEnd(): Unit

  def isRunning: Boolean = running

  override def init(): Unit = {
    if (!running) {
      onStart()
      running = true
    }
  }

  override def abort(): Unit = {
    if (running) {
      onEnd()
      running = false
    }
  }

  def then(that: FiniteTask): FiniteTask = {
    new SequentialTask(this, that)
  }

  def withTimeout(timeout: Time)(implicit clock: Clock): FiniteTask = {
    new TimeoutFiniteTask(this, timeout, clock)
  }
}
