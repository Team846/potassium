package com.lynbrookrobotics.potassium.vision

import com.lynbrookrobotics.potassium.Signal
import com.lynbrookrobotics.potassium.streams._
import org.scalatest.FunSuite
import squants.{Angle, Dimensionless, Length, Percent}
import squants.space.{Degrees, Feet}
import squants.time.Milliseconds

class TargetTrackingTest extends FunSuite {

  val period = Milliseconds(5)
  val periodsPerSecond: Int = (1 / period.toSeconds).toInt

  test("cube with measured percent area 15.6 results in distance 2.75 feet away") {
    val (percentArea, pubPercentArea) = Stream.manual[Option[Dimensionless]]
    val camProps: Signal[VisionProperties] = Signal.constant(VisionProperties(Degrees(0), Feet(10.8645)))
    val targetDistance: Option[Length] = Some(Feet(2.75))

    val distanceToTarget = VisionTargetTracking.distanceToTarget(percentArea, camProps)

    var lastTargetPosition: Option[Length] = Option(Feet(0))

    distanceToTarget.foreach(lastTargetPosition = _)

    for(_ <- 1 to periodsPerSecond){
      pubPercentArea(Some(Percent(15.6)))
    }

    assert(targetDistance.get - lastTargetPosition.get < Feet(0.1))
  }

  test("angle to target measured as 5 degrees for input of 10 degrees and offset of -5 degrees") {
    val (angle, pubAngle) = Stream.manual[Angle]
    val camProps: Signal[VisionProperties] = Signal.constant(VisionProperties(Degrees(-5), Feet(10.8645)))
    val targetAngle: Angle = Degrees(-5)

    val angleToTarget = VisionTargetTracking.angleToTarget(angle, camProps)
    var lastAngle = Degrees(0)

    angleToTarget.foreach(lastAngle = _)

    for(_ <- 1 to periodsPerSecond){
      pubAngle(Degrees(10))
    }

    assert(targetAngle - lastAngle < Degrees(1))
  }
}
