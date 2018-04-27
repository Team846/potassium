package com.lynbrookrobotics.potassium

import org.scalatest.FunSuite
import com.lynbrookrobotics.potassium.events.{ImpulseEvent, ImpulseEventSource}
import com.lynbrookrobotics.potassium.tasks.ContinuousTask
import com.lynbrookrobotics.potassium.streams._

class ContinuousEventTest extends FunSuite {
  val eventUpdateSource = new ImpulseEventSource
  implicit val eventUpdateEvent: ImpulseEvent = eventUpdateSource.event

  test("Start and end impulse Stream events are fired correctly") {
    val (streamOfCondition, publishCondition) = Stream.manual[Boolean]

    val event = streamOfCondition.eventWhen(identity)
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

    val event = streamOfCondition.eventWhen(identity)
    publishCondition(false)

    var callbackRun = false
    event.foreach(() => callbackRun = true)

    assert(!callbackRun)

    publishCondition(true)

    assert(callbackRun)
  }

  test("Mapped continuous task from Stream event launched correctly") {
    val (streamOfCondition, publishCondition) = Stream.manual[Boolean]

    val event = streamOfCondition.eventWhen(identity)
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

    val eventA = conditionAStream.eventWhen(identity)
    val eventB = conditionBStream.eventWhen(identity)

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

    val event = streamOfCondition.eventWhen(identity)

    publishCondition(true)

    (!event).foreach(() => {
      callbackRun = true
    })

    assert(!callbackRun)

    publishCondition(false)
    assert(callbackRun)
  }
}
