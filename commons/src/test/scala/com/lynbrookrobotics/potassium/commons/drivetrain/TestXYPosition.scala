package com.lynbrookrobotics.potassium.commons.drivetrain

import com.lynbrookrobotics.potassium.streams.{Periodic, Stream}
import com.lynbrookrobotics.potassium.commons.cartesianPosition.XYPosition
import com.lynbrookrobotics.potassium.testing.ClockMocking
import com.lynbrookrobotics.potassium.units.Point
import org.scalatest.FunSuite
import squants.{Angle, Length, Velocity}
import squants.motion.{AngularVelocity, DegreesPerSecond, FeetPerSecond}
import squants.space.{Degrees, Feet}
import squants.time.{Milliseconds, Seconds}

class TestXYPosition extends FunSuite{
  val period = Milliseconds(5)
  val periodsPerSecond = (1 / period.toSeconds).toInt

  test("Test moving straight at 1 ft/s for 1 sec results in moving 1 ft forward") {
    implicit val (clock, clockTrigger) = ClockMocking.mockedClockTicker

    val (velocity, pubVelocity) = Stream.manual[Velocity](Periodic(period))
    val distance = velocity.integral

    val (angle, pubAngle) = Stream.manual[Angle]

    val targetPosition = Point(Feet(0), Feet(1))

    val position = XYPosition(angle, distance)
    val simpsonsPosition = XYPosition.positionWithSimpsons(
      angle,
      distance)

    var lastPosition: Point = _
    var lastSimpsonPosition: Point = _

    position.foreach(lastPosition = _)
    simpsonsPosition.foreach(lastSimpsonPosition = _)

    for(_ <- 1 to periodsPerSecond){
      clockTrigger.apply(period)
      pubVelocity.apply(FeetPerSecond(1))
      pubAngle.apply(Degrees(90))
    }

    implicit val tolerance = Feet(0.05)
    assert(lastPosition ~= targetPosition)
    assert(lastSimpsonPosition ~= targetPosition)
  }

  test("Test moving 45 degrees at 1 ft/s for 1 sec results in (sqrt(2)/2,sqrt(2)/2) ") {
    val (velocity, pubVel) = Stream.manual[Velocity](Periodic(period))
    val distance = velocity.integral

    val (angle, pubAngle) = Stream.manual[Angle]
    val targetPosition = Point(
      Feet(1 * Degrees(45).cos),
      Feet(1 * Degrees(45).sin))

    val position = XYPosition(angle, distance)
    val simpsonsPosition = XYPosition.positionWithSimpsons(
      angle,
      distance)

    var lastPose: Point = _
    var lastSimpsonsPose: Point = _

    position.foreach(lastPose = _)
    simpsonsPosition.foreach(lastSimpsonsPose = _)

    for (_ <- 1 to periodsPerSecond) {
      pubVel(FeetPerSecond(1))
      pubAngle(Degrees(45))
    }

    implicit val tolerance = Feet(0.05)
    assert(lastPose ~= targetPosition)
    assert(lastSimpsonsPose ~= targetPosition)
  }

  test("Test rotating 90 degrees/s with radius 1 starting angle 90 " +
       "degrees for 1 sec results in (-1,1) ") {
    val (angularSpeed, pubTurnSpeed) = Stream.manual[AngularVelocity](Periodic(period))
    val angle = angularSpeed.integral.map(_ + Degrees(90))

    val distance = angle.map{ angle =>
      (angle - Degrees(90)).toRadians * Feet(1)
    }

    val targetPosition = Point(
      Feet(-1),
      Feet(1))

    val position = XYPosition(angle, distance)
    val simpsonsPosition = XYPosition.positionWithSimpsons(
      angle,
      distance)

    var lastPose: Point = _
    var lastSimpPose: Point = _

    position.foreach(lastPose = _)
    simpsonsPosition.foreach(lastSimpPose = _)

    for(_ <- 1 to periodsPerSecond){
      pubTurnSpeed(DegreesPerSecond(90))
    }

    implicit val tolerance = Feet(0.05)
    assert(lastPose ~= targetPosition)
    assert(lastSimpPose ~= targetPosition)
  }
}
