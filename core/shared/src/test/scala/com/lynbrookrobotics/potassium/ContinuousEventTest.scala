package com.lynbrookrobotics.potassium

import org.scalatest.FunSuite
import com.lynbrookrobotics.potassium.events.ImpulseEventSource
import com.lynbrookrobotics.potassium.tasks.ContinuousTask
import com.lynbrookrobotics.potassium.streams._
import com.lynbrookrobotics.potassium.streams

class ContinuousEventTest extends FunSuite {
  val eventUpdateSource = new ImpulseEventSource
  implicit val eventUpdateEvent = eventUpdateSource.event

  test("Start and end impulse Stream events are fired correctly") {
    val (streamOfCondition, publishCondition) = Stream.manual[Boolean]

    val event = streamOfCondition.eventWithCondition(identity)
    publishCondition(false)

    var onStart = false
    var onEnd = false

    event.onStart.foreach(() => onStart = true)
    event.onEnd.foreach(() => onEnd = true)

    assert(!onStart && !onEnd)

    publishCondition(true)

    assert(onStart && !onEnd)

    publishCondition(false)

    assert(onStart && onEnd)
  }

  test("On ticking callback from Stream event fired correctly") {
    val (streamOfCondition, publishCondition) = Stream.manual[Boolean]

    val event = streamOfCondition.eventWithCondition(identity)
    publishCondition(false)

    var callbackRun = false
    event.foreach(() => callbackRun = true)

    assert(!callbackRun)

    publishCondition(true)

    assert(callbackRun)
  }

  test("Mapped continuous task from Stream event launched correctly") {
    val (streamOfCondition, publishCondition) = Stream.manual[Boolean]

    val event = streamOfCondition.eventWithCondition(identity)
    publishCondition(false)

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

    assert(!taskStarted && !taskEnded)

    publishCondition(true)

    assert(taskStarted && !taskEnded)

    publishCondition(false)

    assert(taskStarted && taskEnded)
  }

  test("Combined events from Stream fired correctly") {
    val (conditionAStream, publishConditionA) = Stream.manual[Boolean]

    val (conditionBStream, publishConditionB) = Stream.manual[Boolean]

    val eventA = conditionAStream.eventWithCondition(identity)
    val eventB = conditionBStream.eventWithCondition(identity)

    var callbackRun = false
    (eventA && eventB).foreach(() => {
      callbackRun = true
    })

    publishConditionA(false)
    publishConditionB(false)

    assert(!callbackRun)

    publishConditionA(true)
    assert(!callbackRun)

    publishConditionA(false)
    publishConditionB(true)
    assert(!callbackRun)

    publishConditionA(true)
    publishConditionB(true)
    assert(callbackRun)
  }

  test("Opposite event from Stream fired correctly") {
    val (streamOfCondition, publishCondition) = Stream.manual[Boolean]


    var callbackRun = false

    val event = streamOfCondition.eventWithCondition(identity)

    publishCondition(true)

    (!event).foreach(() => {
      callbackRun = true
    })

    assert(!callbackRun)

    publishCondition(false)
    assert(callbackRun)
  }

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

  test("Mapped continuous task signal launched correctly") {
    var condition = false
    val event = Signal(condition).filter(identity)

    var taskStarted = false
    var taskEnded = false

    event.foreach(Signal(new ContinuousTask {
      override def onStart(): Unit = {
        taskStarted = true
      }

      override def onEnd(): Unit = {
        taskEnded = true
      }
    }))

    eventUpdateSource.fire()
    assert(!taskStarted && !taskEnded)

    condition = true

    eventUpdateSource.fire()
    assert(taskStarted && !taskEnded)

    condition = false

    eventUpdateSource.fire()
    assert(taskStarted && taskEnded)
  }

  test("Combined events fired correctly") {
    var conditionA = false
    var conditionB = false

    var callbackRun = false

    val eventA = Signal(conditionA).filter(identity)
    val eventB = Signal(conditionB).filter(identity)

    (eventA && eventB).foreach(() => {
      callbackRun = true
    })

    eventUpdateSource.fire()
    assert(!callbackRun)

    conditionA = true
    eventUpdateSource.fire()
    assert(!callbackRun)

    conditionA = false
    conditionB = true
    eventUpdateSource.fire()
    assert(!callbackRun)

    conditionA = true
    conditionB = true
    eventUpdateSource.fire()
    assert(callbackRun)
  }

  test("Opposite event fired correctly") {
    var condition = true

    var callbackRun = false

    val event = Signal(condition).filter(identity)

    (!event).foreach(() => {
      callbackRun = true
    })

    eventUpdateSource.fire()
    assert(!callbackRun)

    condition = false
    eventUpdateSource.fire()
    assert(callbackRun)
  }
}
