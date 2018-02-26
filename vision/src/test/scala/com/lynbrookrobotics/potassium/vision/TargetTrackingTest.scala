package com.lynbrookrobotics.potassium.vision

import com.lynbrookrobotics.potassium.Signal
import com.lynbrookrobotics.potassium.streams._
import org.scalatest.FunSuite
import squants.{Angle, Dimensionless, Length, Percent}
import squants.space.{Degrees, Feet}

class TargetTrackingTest extends FunSuite {
  test("Cube with measured percent area 15.6 results in distance 2.75 feet away") {
    val (percentArea, pubPercentArea) = Stream.manual[Option[Dimensionless]]
    val targetDistance = Some(Feet(1))
    val targeter = new VisionTargetTracking(Signal.constant(VisionProperties(Degrees(0), Feet(1))))

    val distanceToTarget = targeter.distanceToTarget(percentArea)
    var lastTargetPosition: Option[Length] = Some(Feet(0))

    distanceToTarget.foreach(lastTargetPosition = _)
    pubPercentArea(Some(Percent(1)))

    implicit val tolerance: Length = Feet(0.1)
    assert(targetDistance.get ~= lastTargetPosition.get)
  }

  test("Angle to target measured as 5 degrees for input of 10 degrees and offset of -5 degrees") {
    val (angle, pubAngle) = Stream.manual[Angle]
    val targeter = new VisionTargetTracking(
      Signal.constant(VisionProperties(
          Degrees(-5),
          Feet(1)))
    )
    val angleToTarget = targeter.compassAngleToTarget(angle)
    var lastAngle = Degrees(0)

    angleToTarget.foreach(lastAngle = _)
    pubAngle(Degrees(10))

    implicit val tolerance = Degrees(0.00001)
    assert(Degrees(5) ~= lastAngle)
  }
}
