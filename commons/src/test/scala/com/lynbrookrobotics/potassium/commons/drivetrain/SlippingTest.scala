package com.lynbrookrobotics.potassium.commons.drivetrain

import com.lynbrookrobotics.potassium.ClockMocking
import com.lynbrookrobotics.potassium.streams.Stream
import com.lynbrookrobotics.potassium.commons.position.Slipping
import org.scalatest.FunSuite
import squants.Velocity
import squants.motion._
import squants.space.{Feet, Length}
import squants.time.Milliseconds

class SlippingTest extends FunSuite {

  implicit val (clock, triggerClock) = ClockMocking.mockedClockTicker
  val period = Milliseconds(5)

  test("left and right slipping") {
    val angularVelocity: Stream[AngularVelocity] = Stream.periodic(period)(DegreesPerSecond(5) * clock.currentTime.value)
    val linearAcceleration: Stream[Acceleration] = Stream.periodic(period)(FeetPerSecondSquared(5))
    val leftEncoderVelocity: Stream[Velocity] = Stream.periodic(period)(FeetPerSecond(2) * clock.currentTime.value)
    val rightEncoderVelocity: Stream[Velocity] = Stream.periodic(period)(FeetPerSecond(9.5) * clock.currentTime.value)
    val distanceFromAccelerometerLeft: Length = Feet(0.5) // calc = 2.5
    val distanceFromAccelerometerRight: Length = Feet(1) // calc = 10
    val allowedAccelerationDeviation: Acceleration = MetersPerSecondSquared(1)

    val isSlipping = Slipping.slippingDetection( angularVelocity,
      linearAcceleration,
      leftEncoderVelocity,
      rightEncoderVelocity,
      distanceFromAccelerometerLeft,
      distanceFromAccelerometerRight,
      allowedAccelerationDeviation)

    isSlipping.foreach(x => assert(x._1 && x._2))

    triggerClock.apply(period)
    triggerClock.apply(period)
    triggerClock.apply(period)
    triggerClock.apply(period)
    triggerClock.apply(period)
    triggerClock.apply(period)
  }

//  test("left and right not slipping") {
//    val angularVelocity: Stream[AngularVelocity] = null
//    val linearAcceleration: Stream[Acceleration] = null
//    val encoderVelocity: Stream[Velocity] = null
//    val distanceFromAccelerometerLeft: Length = null
//    val distanceFromAccelerometerRight: Length = null
//    val allowedAccelerationDeviation: Acceleration = null
//
//    val isSlipping = Slipping.slippingDetection( angularVelocity,
//      linearAcceleration,
//      encoderVelocity,
//      distanceFromAccelerometerLeft,
//      distanceFromAccelerometerRight,
//      allowedAccelerationDeviation)
//
//    triggerClock.apply(period)
//    triggerClock.apply(period)
//
//    isSlipping.foreach(x => assert(!x._1 && !x._2))
//  }
}
