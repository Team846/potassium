package com.lynbrookrobotics.potassium.commons.drivetrain

import com.lynbrookrobotics.potassium.Signal
import com.lynbrookrobotics.potassium.commons.cartesianPosition.XYPosition
import com.lynbrookrobotics.potassium.units.Point
import org.scalatest.FunSuite
import squants.motion.{DegreesPerSecond, FeetPerSecond}
import squants.space.{Degrees, Feet}
import squants.time.{Milliseconds, Seconds}

class TestXYPosition extends FunSuite{
  val period = Milliseconds(5)
  val periodsPerSecond = (1 / period.toSeconds).toInt

  test("Test moving straight at 1 ft/s for 1 sec results in moving 1 ft forward") {
    val periodicDistance = Signal.constant(FeetPerSecond(1)).toPeriodic.integral
    val distance = periodicDistance.peek.map(_.getOrElse(Feet(0)))

    val angle = Signal.constant(Degrees(90))

    val targetPosition = Point(Feet(0), Feet(1))

    val position = XYPosition(angle, distance)
    val simpsonsPosition = XYPosition.positionWithSimpsons(
      angle,
      distance)

    for(_ <- 1 to periodsPerSecond){
      periodicDistance.currentValue(period)
      position.currentValue(period)
      simpsonsPosition.currentValue(period)
    }

    implicit val tolerance = Feet(0.05)
    assert(position.peek.get.get ~= targetPosition)
    assert(simpsonsPosition.peek.get.get ~= targetPosition)
  }

  test("Test moving 45 degrees at 1 ft/s for 1 sec results in (sqrt(2)/2,sqrt(2)/2) ") {
    val periodicDistance = Signal.constant(FeetPerSecond(1)).toPeriodic.integral
    val distance = periodicDistance.peek.map(_.getOrElse(Feet(0)))

    val angle = Signal.constant(Degrees(45))
    val targetPosition = Point(
      Feet(1 * Degrees(45).cos),
      Feet(1 * Degrees(45).sin))

    val position = XYPosition(angle, distance)
    val simpsonsPosition = XYPosition.positionWithSimpsons(
      angle,
      distance)

    for (_ <- 1 to periodsPerSecond) {
      periodicDistance.currentValue(period)
      position.currentValue(period)
      simpsonsPosition.currentValue(period)
    }

    implicit val tolerance = Feet(0.05)
    assert(position.peek.get.get ~= targetPosition)
    assert(simpsonsPosition.peek.get.get ~= targetPosition)
  }

  test("Test rotating 90 degrees/s with radius 1 starting angle 90 " +
       "degrees for 1 sec results in (-1,1) ") {
    val angularSpeed = Signal.constant(DegreesPerSecond(90))
    val periodicAngle = angularSpeed.toPeriodic.integral.map(_ + Degrees(90))
    val angle = periodicAngle.peek.map(_.getOrElse(Degrees(90)))

    val periodicDistance = periodicAngle.map{ angle =>
      (angle - Degrees(90)).toRadians * Feet(1)
    }
    val distance = periodicDistance.peek.map(_.getOrElse(Feet(0)))

    val targetPosition = Point(
      Feet(-1),
      Feet(1))

    val position = XYPosition(angle, distance)
    val simpsonsPosition = XYPosition.positionWithSimpsons(
      angle,
      distance)

    for(_ <- 1 to periodsPerSecond){
      periodicDistance.currentValue(period)
      position.currentValue(period)
      simpsonsPosition.currentValue(period)
    }

    implicit val tolerance = Feet(0.05)
    assert(position.peek.get.get ~= targetPosition)
    assert(simpsonsPosition.peek.get.get ~= targetPosition)
  }
}
