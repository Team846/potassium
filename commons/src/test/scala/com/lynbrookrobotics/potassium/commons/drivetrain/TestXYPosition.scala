package com.lynbrookrobotics.potassium.commons.drivetrain

import com.lynbrookrobotics.potassium.Signal
import com.lynbrookrobotics.potassium.commons.cartesianPosition.XYPosition
import com.lynbrookrobotics.potassium.units.Point
import org.scalatest.FunSuite
import squants.motion.{DegreesPerSecond, FeetPerSecond}
import squants.space.{Degrees, Feet}
import squants.time.Milliseconds

class TestXYPosition extends FunSuite{
  val origin = new Point(Feet(0), Feet(0))

  test("Test moving straight at 2 ft/s for 1 sec results in moving 2 ft forward") {
    implicit val tolerance = Feet(0.05)
    val distance = Signal.constant(FeetPerSecond(2)).toPeriodic.integral

    val targetPosition = new Point(Feet(0), Feet(2))

    val position = XYPosition(
      Signal.constant(Degrees(90)),
      distance.peek.map(_.getOrElse(Feet(0))))

    for(_ <- 1 to 200){
      distance.currentValue(Milliseconds(5))
      position.currentValue(Milliseconds(5))
    }

    assert(position.peek.get.get ~= targetPosition)
  }

  test("Test moving 45 degrees at 1 ft/s for 1 sec results in (sqrt(2)/2,sqrt(2)/2) ") {
    implicit val tolerance = Feet(0.05)
    val distance = Signal.constant(FeetPerSecond(1)).toPeriodic.integral

    val angle = Signal.constant(Degrees(45))
    val targetPosition = new Point(
      Feet(Math.cos(angle.get.toRadians)),
      Feet(Math.sin(angle.get.toRadians)))

    val position = XYPosition(angle, distance.peek.map(_.getOrElse(Feet(0))))

    for (_ <- 1 to 200) {
      distance.currentValue(Milliseconds(5))
      position.currentValue(Milliseconds(5))
    }

    assert(position.peek.get.get ~= targetPosition)
  }

  test("Test rotating 90 degrees/s with radius 1 starting angle 90 " +
       "degrees for 4 sec results in (0,0) ") {
    implicit val tolerance = Feet(0.05)
    val periodicAngularSpeed = Signal.constant(DegreesPerSecond(90)).toPeriodic
    val angle = periodicAngularSpeed.integral.map(_ + Degrees(90))
    val distance = angle.map{ angle =>
      (angle.toRadians - Degrees(90).toRadians) * Feet(1)
    }

    val targetPosition = new Point(
      Feet(0),
      Feet(0))

    val position = XYPosition(
      Signal(angle.peek.get.getOrElse(Degrees(90))),
      distance.peek.map(_.getOrElse(Feet(0))))

    for(_ <- 1 to 800){
      distance.currentValue(Milliseconds(5))
      position.currentValue(Milliseconds(5))
    }

    assert(position.peek.get.get ~= targetPosition)
  }
}
