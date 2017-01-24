package com.lynbrookrobotics.potassium.config

import org.scalatest.FunSuite

class TwoWaySignalTest extends FunSuite {
  test("Forward propagation works with map") {
    val sig = TwoWaySignal(0)
    val mappedSignal = sig.map(_ + 1, (oldValue, newValue: Int) => newValue - 1)
    assert(mappedSignal.value == 1)
  }

  test("Reverse propagation works with map") {
    val sig = TwoWaySignal(0)
    val mappedSignal = sig.map(_ + 1, (oldValue, newValue: Int) => newValue - 1)
    mappedSignal.value = 5
    assert(sig.value == 4)
  }
}
