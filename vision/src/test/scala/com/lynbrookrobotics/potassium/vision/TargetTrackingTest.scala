package com.lynbrookrobotics.potassium.vision

import com.lynbrookrobotics.potassium.ClockMocking
import com.lynbrookrobotics.potassium.units.Point
import org.scalatest.FunSuite
import squants.{Angle, Length}
import squants.space.{Degrees, Feet}
import squants.time.Milliseconds

class TargetTrackingTest extends FunSuite {

  val period = Milliseconds(5)
  val periodsPerSecond: Int = (1 / period.toSeconds).toInt

  test("Cube -- target height : 11 in, camera height : 3 ft 11 in, y offset ang : -45`, no other" +
    "--> 3 ft ahead") {
    implicit val (clock, clockTrigger) = ClockMocking.mockedClockTicker

    val (yAngle, pubYAngle) = Stream.manualWithTime[Angle]//Stream.manualWithTime[Angle](Periodic(period))
    val (xAngle, pubXAngle) = Stream.manualWithTime[Angle]

    implicit val camProps: CameraProperties = CameraProperties(Degrees(0), Degrees(0), Feet(47.0 / 12.0), Feet(11.0 / 12.0))

    val target: Point = Point(Feet(0), Feet(3))

    val cubePosition = new TargetTracking(xAngle.zip(yAngle)).target

    var lastCubePosition = Point(Feet(0), Feet(0))

    cubePosition.foreach(lastCubePosition = _)


    for(_ <- 1 to periodsPerSecond){
      clockTrigger.apply(period)
      pubXAngle.apply(Degrees(0))
      pubYAngle.apply(Degrees(-45))
    }

    println(lastCubePosition)

    implicit val tolerance: Length = Feet(0.1)
    assert(target ~= lastCubePosition)
  }

  test("Cube -- target height : 13 in, camera height : 6 ft 11 in, " +
    "y offset ang : -42`, x offset ang : 6`, " +
    "cam x offset ang : 12`, cam y offset ang : -23`" +
    "--> 0.841 ft 2.587 in ahead") {
    implicit val (clock, clockTrigger) = ClockMocking.mockedClockTicker

    val (yAngle, pubYAngle) = Stream.manualWithTime[Angle]//Stream.manualWithTime[Angle](Periodic(period))
    val (xAngle, pubXAngle) = Stream.manualWithTime[Angle]

    implicit val camProps: CameraProperties = CameraProperties(Degrees(12), Degrees(-23), Feet(83.0 / 12.0), Feet(13.0 / 12.0))

    val target: Point = Point(Feet(0.841), Feet(2.587))

    val cubePosition = new TargetTracking(xAngle.zip(yAngle)).target

    var lastCubePosition = Point(Feet(0), Feet(0))

    cubePosition.foreach(lastCubePosition = _)

    for(_ <- 1 to periodsPerSecond){
      clockTrigger.apply(period)
      pubXAngle.apply(Degrees(6))
      pubYAngle.apply(Degrees(-42))
    }
    println(lastCubePosition)
    implicit val tolerance: Length = Feet(0.1)
    assert(target ~= lastCubePosition)
  }
}
    println(lastCubePosition)

    implicit val tolerance: Length = Feet(0.1)
    assert(target ~= lastCubePosition)
  }
}
