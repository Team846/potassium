package com.lynbrookrobotics.potassium.streams

import org.scalatest.FunSuite

class ZippedStreamTest extends FunSuite {
  test("Zipped stream is triggered correctly") {
    val (a, pushA) = Stream.manual[Int]
    val (b, pushB) = Stream.manual[Int]
    val zipped = a.zip(b)

    var lastValue = -1

    zipped.foreach { case (aVal, bVal) =>
      lastValue = aVal * bVal
    }

    assert(lastValue == -1)

    pushA(1)

    assert(lastValue == -1)

    pushB(2)

    assert(lastValue == 2)

    pushB(3)

    assert(lastValue == 2)

    pushA(2)

    assert(lastValue == 6)

    pushA(3)

    assert(lastValue == 6)

    pushA(4)

    assert(lastValue == 6)

    pushB(2)

    assert(lastValue == 8)

    pushB(1)

    assert(lastValue == 8)

    pushB(2)

    assert(lastValue == 8)

    pushA(2)

    assert(lastValue == 4)
  }

}
