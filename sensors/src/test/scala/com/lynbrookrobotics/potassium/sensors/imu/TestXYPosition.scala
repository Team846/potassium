package com.lynbrookrobotics.potassium.sensors.imu

import com.lynbrookrobotics.potassium.Signal
import com.lynbrookrobotics.potassium.sensors.position.xyPosition
import com.lynbrookrobotics.potassium.units.Point
import org.scalatest.FunSuite
import squants.time.Milliseconds
import squants.motion.{DegreesPerSecond, FeetPerSecond}
import squants.space.{Degrees, Feet}
import squants.time.Seconds

class TestXYPosition extends FunSuite{
  val origin = new Point(Feet(0), Feet(0))

  test("Test moving straight at 2 ft/s for 1 sec results in moving 2 ft forward") {
    lazy val startTime = System.currentTimeMillis
    def timePassed = Milliseconds(System.currentTimeMillis - startTime)

    val distance = Signal(timePassed * FeetPerSecond(2))
    val targetPosition = new Point(Feet(0), Feet(2))

    val position = xyPosition(
      origin,
      Signal.constant(Degrees(90)),
      distance.toPeriodic
    )

    var i = 0
    while (timePassed <= Seconds(1)){
      if(i % 10000000 == 0) position.currentValue(Seconds(0.1))
      i = i + 1
    }

    assert(position.currentValue(Seconds(0.1)) == targetPosition)
  }

  test("Test moving 45 degrees at 1 ft/s for 1 sec results in (sqrt(2)/2,sqrt(2)/2) ") {
    lazy val startTime = System.currentTimeMillis
    def timePassed = Seconds((System.currentTimeMillis - startTime) / 1000D)

    val distance = Signal(timePassed * FeetPerSecond(1))
    val angle = Signal.constant(Degrees(45))
    val targetPosition = new Point(
      Feet(Math.cos(angle.get.toRadians)),
      Feet(Math.sin(angle.get.toRadians)))

    val position = xyPosition(
      origin,
      angle,
      distance.toPeriodic
    )

    var i = 0
    while (timePassed <= Seconds(1)){
      if(i % 1000000 == 0) position.currentValue(Seconds(0.1))
      i = i + 1
    }

    assert(position.currentValue(Seconds(0.1)) == targetPosition)
  }

  test("Test rotating 90 degrees/s with radius 1, starting angle 90 degrees for" +
    " 1 sec results in (-1,1) ") {
    lazy val startTime = System.currentTimeMillis
    def timePassed = Seconds((System.currentTimeMillis - startTime) / 1000D)

    val angle = Signal(Degrees(90) + timePassed * DegreesPerSecond(90))
    val distance = Signal(Feet(1) * (angle.get.toRadians - Degrees(90).toRadians))

    val targetPosition = new Point(
      Feet(-1),
      Feet(1))

    val position = xyPosition(
      origin,
      angle,
      distance.toPeriodic
    )

    var i = 0
    while (timePassed <= Seconds(1)){
      if(i % 1000000 == 0) position.currentValue(Seconds(0.1))
      i =  i + 1
    }

    assert(position.currentValue(Seconds(0.1)) == targetPosition)
  }
}
