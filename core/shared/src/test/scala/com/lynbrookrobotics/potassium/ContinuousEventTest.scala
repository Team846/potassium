package com.lynbrookrobotics.potassium

import org.scalatest.FunSuite
import com.lynbrookrobotics.potassium.events.ImpulseEventSource
import com.lynbrookrobotics.potassium.tasks.ContinuousTask

class ContinuousEventTest extends FunSuite {
  val eventUpdateSource = new ImpulseEventSource
  implicit val eventUpdateEvent = eventUpdateSource.event

  test("Start and end impulse events are fired correctly") {
    var condition = false
    val event = Signal(condition).filter(identity)

    var onStart = false
    var onEnd = false

    event.onStart.foreach(() => onStart = true)
    event.onEnd.foreach(() => onEnd = true)

    eventUpdateSource.fire()
    assert(!onStart && !onEnd)

    condition = true

    eventUpdateSource.fire()
    assert(onStart && !onEnd)

    condition = false

    eventUpdateSource.fire()
    assert(onStart && onEnd)
  }

  test("On ticking callback fired correctly") {
    var condition = false
    val event = Signal(condition).filter(identity)

    var callbackRun = false
    event.foreach(() => callbackRun = true)

    eventUpdateSource.fire()
    assert(!callbackRun)

    condition = true

    eventUpdateSource.fire()
    assert(callbackRun)
  }

  test("Mapped continuous task launched correctly") {
    var condition = false
    val event = Signal(condition).filter(identity)

    var taskStarted = false
    var taskEnded = false

    event.foreach(new ContinuousTask {
      override def onStart(): Unit = {
        taskStarted = true
      }

      override def onEnd(): Unit = {
        taskEnded = true
      }
    })

    eventUpdateSource.fire()
    assert(!taskStarted && !taskEnded)

    condition = true

    eventUpdateSource.fire()
    assert(taskStarted && !taskEnded)

    condition = false

    eventUpdateSource.fire()
    assert(taskStarted && taskEnded)
  }
}
