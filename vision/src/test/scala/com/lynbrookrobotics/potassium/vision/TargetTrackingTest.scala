package com.lynbrookrobotics.potassium.vision

import com.lynbrookrobotics.potassium.Signal
import com.lynbrookrobotics.potassium.streams._
import org.scalatest.FunSuite
import squants.{Angle, Dimensionless, Length, Percent}
import squants.space.{Degrees, Feet}
import squants.time.Milliseconds

class TargetTrackingTest extends FunSuite {
  test("Cube with measured percent area 15.6 results in distance 2.75 feet away") {
    val (percentArea, pubPercentArea) = Stream.manual[Option[Dimensionless]]
    val targetDistance: Option[Length] = Some(Feet(2.75))
    val targeter: VisionTargetTracking = new VisionTargetTracking(Signal.constant(Degrees(0)), Signal.constant(Feet(10.8645)))
    val distanceToTarget = targeter.distanceToTarget(percentArea)
    var lastTargetPosition: Option[Length] = Option(Feet(0))

    distanceToTarget.foreach(lastTargetPosition = _)
    pubPercentArea(Some(Percent(15.6)))

    implicit val tolerance: Length = Feet(0.1)
    assert(targetDistance.get ~= lastTargetPosition.get)
  }

  test("Angle to target measured as 5 degrees for input of 10 degrees and offset of -5 degrees") {
    val (angle, pubAngle) = Stream.manual[Angle]
    val targetAngle: Angle = Degrees(-5)
    val targeter: VisionTargetTracking = new VisionTargetTracking(Signal.constant(Degrees(-5)), Signal.constant(Feet(10.8645)))
    val angleToTarget = targeter.angleToTarget(angle)
    var lastAngle = Degrees(0)

    angleToTarget.foreach(lastAngle = _)
    pubAngle(Degrees(10))

    implicit val tolerance: Angle = Degrees(1)
    assert(targetAngle ~= lastAngle)
  }
}
