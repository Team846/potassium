package com.lynbrookrobotics.potassium

import org.scalatest.FunSuite
import com.lynbrookrobotics.potassium.events.ImpulseEventSource

class ImpluseEventTest extends FunSuite {
  test("Impulse event source dispatches event to all listeners") {
    val source = new ImpulseEventSource
    val event = source.event

    var firedCount = 0
    event.foreach(() => firedCount += 1)
    event.foreach(() => firedCount += 1)

    source.fire()
    assert(firedCount == 2)

    event.foreach(() => firedCount += 1)

    source.fire()
    assert(firedCount == 5)
  }
}
