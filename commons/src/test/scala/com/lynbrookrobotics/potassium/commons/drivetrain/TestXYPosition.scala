package com.lynbrookrobotics.potassium.commons.drivetrain

import com.lynbrookrobotics.potassium.ClockMocking
import com.lynbrookrobotics.potassium.streams.Stream
import com.lynbrookrobotics.potassium.commons.cartesianPosition.XYPosition
import com.lynbrookrobotics.potassium.units.Point
import org.scalatest.FunSuite
import squants.{Angle, Velocity}
import squants.motion.{AngularVelocity, DegreesPerSecond, FeetPerSecond}
import squants.space.{Degrees, Feet}
import squants.time.Milliseconds

class TestXYPosition extends FunSuite {
  val period = Milliseconds(5)
  val periodsPerSecond = (1 / period.toSeconds).toInt
  val unitializedPose = Point(Feet(-10), Feet(-10))

  test("Test moving straight at 1 ft/s for 1 sec results in moving 1 ft forward") {
    implicit val (clock, clockTrigger) = ClockMocking.mockedClockTicker

    val (velocityAngle, pubVelocityAngle) = Stream.manualWithTime[(Velocity, Angle)](period)
    val velocity = velocityAngle.map(_._1)
    val distance = velocity.integral

    val angle = velocityAngle.map(_._2)

    val targetPosition = Point(Feet(0), Feet(1))

    val position = XYPosition(angle, distance)
    val simpsonsPosition = XYPosition.positionWithSimpsons(angle, velocity)
    val circularPosition = XYPosition.circularTracking(angle, distance)

    var lastPosition = unitializedPose
    var lastSimpsonPosition = unitializedPose
    var lastCircularPosition = unitializedPose

    position.foreach(lastPosition = _)
    simpsonsPosition.foreach(lastSimpsonPosition = _)
    circularPosition.foreach(lastCircularPosition = _)

    for (_ <- 1 to periodsPerSecond) {
      clockTrigger.apply(period)
      pubVelocityAngle.apply((FeetPerSecond(1), Degrees(90)))
    }

    implicit val tolerance = Feet(0.1)
    assert(lastPosition ~= targetPosition)
    assert(lastSimpsonPosition ~= targetPosition)
    assert(lastCircularPosition ~= targetPosition)
  }

  test("Test moving 45 degrees at 1 ft/s for 1 sec results in (sqrt(2)/2,sqrt(2)/2) ") {
    implicit val (clock, clockTrigger) = ClockMocking.mockedClockTicker

    val (velocityAngle, pubVelocityAngle) = Stream.manualWithTime[(Velocity, Angle)](period)
    val velocity = velocityAngle.map(_._1)
    val distance = velocity.integral

    val angle = velocityAngle.map(_._2)
    val targetPosition = Point(Feet(1 * Degrees(45).cos), Feet(1 * Degrees(45).sin))

    val position = XYPosition(angle, distance)
    val simpsonsPosition = XYPosition.positionWithSimpsons(angle, velocity)
    val circularPosition = XYPosition.circularTracking(
      angle,
      distance
    )

    var lastPose = unitializedPose
    var lastSimpsonsPose = unitializedPose
    var lastCircularPose = unitializedPose

    position.foreach(lastPose = _)
    simpsonsPosition.foreach(lastSimpsonsPose = _)
    circularPosition.foreach(lastCircularPose = _)

    for (_ <- 1 to periodsPerSecond) {
      clockTrigger.apply(period)
      pubVelocityAngle((FeetPerSecond(1), Degrees(45)))
    }

    implicit val tolerance = Feet(0.1)
    assert(lastPose ~= targetPosition)
    assert(lastSimpsonsPose ~= targetPosition)
    assert(lastCircularPose ~= targetPosition)
  }

  test(
    "Test rotating 90 degrees/s with radius 1 starting angle 90 " +
      "degrees for 1 sec results in (-1,1) "
  ) {
    implicit val (clock, clockTrigger) = ClockMocking.mockedClockTicker

    val initialAngle = Degrees(90)

    val (angularSpeed, pubTurnSpeed) = Stream.manualWithTime[AngularVelocity](period)
    val angle = angularSpeed.integral.map(_ + initialAngle)

    val distance = angle.map { angle =>
      (angle - initialAngle) onRadius Feet(1)
    }

    val velocity = distance.derivative

    val targetPosition = Point(Feet(-1), Feet(1))

    val position = XYPosition(angle, distance)
    val simpsonsPosition = XYPosition.positionWithSimpsons(angle, velocity)
    val circularPosition = XYPosition.circularTracking(angle, distance)

    var lastPose = unitializedPose
    var lastSimpPose = unitializedPose
    var lastCircularPose = unitializedPose

    position.foreach(lastPose = _)
    simpsonsPosition.foreach(lastSimpPose = _)
    circularPosition.foreach(lastCircularPose = _)

    for (_ <- 1 to periodsPerSecond) {
      clockTrigger.apply(period)
      pubTurnSpeed(DegreesPerSecond(90))
    }

    implicit val tolerance = Feet(0.1)
    assert(lastPose ~= targetPosition)
    assert(lastSimpPose ~= targetPosition)
    assert(lastCircularPose ~= targetPosition, "last circular pose = " + lastCircularPose)
  }
}
