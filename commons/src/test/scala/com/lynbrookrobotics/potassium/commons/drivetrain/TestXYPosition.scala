package com.lynbrookrobotics.potassium.commons.drivetrain

import com.lynbrookrobotics.potassium.ClockMocking
import com.lynbrookrobotics.potassium.streams.{Periodic, Stream}
import com.lynbrookrobotics.potassium.commons.cartesianPosition.XYPosition
import com.lynbrookrobotics.potassium.units.Point
import org.scalatest.FunSuite
import squants.{Angle, Length, Velocity}
import squants.motion.{AngularVelocity, DegreesPerSecond, FeetPerSecond}
import squants.space.{Degrees, Feet}
import squants.time.{Milliseconds, Seconds}

class TestXYPosition extends FunSuite{
  val period = Milliseconds(5)
  val periodsPerSecond = (1 / period.toSeconds).toInt
  val unitializedPose = Point(Feet(-1), Feet(-1))


  test("Test moving straight at 1 ft/s for 1 sec results in moving 1 ft forward") {
    implicit val (clock, clockTrigger) = ClockMocking.mockedClockTicker

    val (velocity, pubVelocity) = Stream.manualWithTime[Velocity](Periodic(period))
    val distance = velocity.integral

    val (angle, pubAngle) = Stream.manualWithTime[Angle](Periodic(period))

    val targetPosition = Point(Feet(0), Feet(1))

    val position = XYPosition(angle, distance)
    val simpsonsPosition = XYPosition.positionWithSimpsons(
      angle,
      distance)

    var lastPosition = unitializedPose
    var lastSimpsonPosition = unitializedPose

    position.foreach(lastPosition = _)
    simpsonsPosition.foreach(lastSimpsonPosition = _)

    for(i <- 1 to periodsPerSecond){
//      println(s"i $i")
      clockTrigger.apply(period)
      pubVelocity.apply(FeetPerSecond(1))
      pubAngle.apply(Degrees(90))
    }

    implicit val tolerance = Feet(0.1)
    assert(lastPosition ~= targetPosition)
    assert(lastSimpsonPosition ~= targetPosition)
  }

  test("Test moving 45 degrees at 1 ft/s for 1 sec results in (sqrt(2)/2,sqrt(2)/2) ") {
    implicit val (clock, clockTrigger) = ClockMocking.mockedClockTicker

    val (velocity, pubVel) = Stream.manualWithTime[Velocity](Periodic(period))
    val distance = velocity.integral

    val (angle, pubAngle) = Stream.manualWithTime[Angle](Periodic(period))
    val targetPosition = Point(
      Feet(1 * Degrees(45).cos),
      Feet(1 * Degrees(45).sin))

    val position = XYPosition(angle, distance)
    val simpsonsPosition = XYPosition.positionWithSimpsons(
      angle,
      distance)

    var lastPose = unitializedPose
    var lastSimpsonsPose = unitializedPose

    position.foreach(lastPose = _)
    simpsonsPosition.foreach(lastSimpsonsPose = _)

    for (_ <- 1 to periodsPerSecond) {
      clockTrigger.apply(period)
      pubVel(FeetPerSecond(1))
      pubAngle(Degrees(45))
    }

    implicit val tolerance = Feet(0.1)
    assert(lastPose ~= targetPosition)
    assert(lastSimpsonsPose ~= targetPosition)
  }

  test("Test rotating 90 degrees/s with radius 1 starting angle 90 " +
       "degrees for 1 sec results in (-1,1) ") {
    implicit val (clock, clockTrigger) = ClockMocking.mockedClockTicker

    val (angularSpeed, pubTurnSpeed) = Stream.manualWithTime[AngularVelocity](Periodic(period))
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

    var lastPose = unitializedPose
    var lastSimpPose = unitializedPose

    position.foreach(lastPose = _)
    simpsonsPosition.foreach(lastSimpPose = _)

    for(_ <- 1 to periodsPerSecond){
      clockTrigger.apply(period)
      pubTurnSpeed(DegreesPerSecond(90))
    }

    implicit val tolerance = Feet(0.1)
    assert(lastPose ~= targetPosition)
    assert(lastSimpPose ~= targetPosition)
  }
}
