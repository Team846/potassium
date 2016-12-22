package com.lynbrookrobotics.potassium.events

import com.lynbrookrobotics.potassium.clock.Clock
import com.lynbrookrobotics.potassium.tasks.{ContinuousTask, Task}
import squants.Time

case class EventPolling(clock: Clock, period: Time)

class ContinuousEvent(condition: => Boolean)(implicit polling: EventPolling) {
  val onStartSource = new ImpulseEventSource
  val onEndSource = new ImpulseEventSource

  val onStart = onStartSource.event
  val onEnd = onEndSource.event

  private var tickingCallbacks: List[() => Unit] = List.empty
  private var isRunning = false

  polling.clock(polling.period) { dt =>
    if (condition) {
      tickingCallbacks.foreach(_.apply())

      if (!isRunning) {
        onStartSource.fire()

        isRunning = true
      }
    } else if (isRunning) {
      onEndSource.fire()
      isRunning = false
    }
  }

  def foreach(onTicking: () => Unit): Unit = {
    tickingCallbacks = onTicking :: tickingCallbacks
  }

  def foreach(task: ContinuousTask): Unit = {
    onStart.foreach(() => Task.executeTask(task))
    onEnd.foreach(() => Task.abortTask(task))
  }
}
