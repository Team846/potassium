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
    val angularVelocity: Stream[AngularVelocity] = Stream.periodic(period)(RadiansPerSecondSquared(5)).integral
    val linearAcceleration: Stream[Acceleration] = Stream.periodic(period)(FeetPerSecondSquared(5))
    val leftEncoderVelocity: Stream[Velocity] = Stream.periodic(period)(FeetPerSecondSquared(2)).integral
    val rightEncoderVelocity: Stream[Velocity] = Stream.periodic(period)(FeetPerSecondSquared(9.5)).integral
    val distanceFromAccelerometerLeft: Length = Feet(0.5) // calc = 2.5
    val distanceFromAccelerometerRight: Length = Feet(1) // calc = 10
    val allowedAccelerationDeviation: Acceleration = MetersPerSecondSquared(1)

    val isSlipping = Slipping.slippingDetection(angularVelocity,
      linearAcceleration,
      leftEncoderVelocity,
      rightEncoderVelocity,
      distanceFromAccelerometerLeft,
      distanceFromAccelerometerRight,
      allowedAccelerationDeviation)
    isSlipping.foreach(x => assert(!x._1 && !x._2))

    triggerClock.apply(period)
    triggerClock.apply(period)
    triggerClock.apply(period)
    triggerClock.apply(period)
    triggerClock.apply(period)
    triggerClock.apply(period)
  }

  test("left and right not slipping") {
    val angularVelocity: Stream[AngularVelocity] = Stream.periodic(period)(RadiansPerSecondSquared(5)).integral
    val linearAcceleration: Stream[Acceleration] = Stream.periodic(period)(FeetPerSecondSquared(5))
    val leftEncoderVelocity: Stream[Velocity] = Stream.periodic(period)(FeetPerSecondSquared(2)).integral
    val rightEncoderVelocity: Stream[Velocity] = Stream.periodic(period)(FeetPerSecondSquared(9.5)).integral
    val distanceFromAccelerometerLeft: Length = Feet(0.5) // calc = 2.5
    val distanceFromAccelerometerRight: Length = Feet(1) // calc = 10
    val allowedAccelerationDeviation: Acceleration = MetersPerSecondSquared(0.1)

    val isSlipping = Slipping.slippingDetection(angularVelocity,
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

  test("right slipping only") {
    val angularVelocity: Stream[AngularVelocity] = Stream.periodic(period)(RadiansPerSecondSquared(5)).integral
    val linearAcceleration: Stream[Acceleration] = Stream.periodic(period)(FeetPerSecondSquared(5))
    val leftEncoderVelocity: Stream[Velocity] = Stream.periodic(period)(FeetPerSecondSquared(2)).integral
    val rightEncoderVelocity: Stream[Velocity] = Stream.periodic(period)(FeetPerSecondSquared(30)).integral
    val distanceFromAccelerometerLeft: Length = Feet(0.5) // calc = 2.5
    val distanceFromAccelerometerRight: Length = Feet(1) // calc = 10
    val allowedAccelerationDeviation: Acceleration = MetersPerSecondSquared(1)

    val isSlipping = Slipping.slippingDetection(angularVelocity,
      linearAcceleration,
      leftEncoderVelocity,
      rightEncoderVelocity,
      distanceFromAccelerometerLeft,
      distanceFromAccelerometerRight,
      allowedAccelerationDeviation)

    isSlipping.foreach(x => assert(!x._1 && x._2))

    triggerClock.apply(period)
    triggerClock.apply(period)
    triggerClock.apply(period)
    triggerClock.apply(period)
    triggerClock.apply(period)
    triggerClock.apply(period)
  }

  test("left slipping only") {
    val angularVelocity: Stream[AngularVelocity] = Stream.periodic(period)(RadiansPerSecondSquared(5)).integral
    val linearAcceleration: Stream[Acceleration] = Stream.periodic(period)(FeetPerSecondSquared(5))
    val leftEncoderVelocity: Stream[Velocity] = Stream.periodic(period)(FeetPerSecondSquared(30)).integral
    val rightEncoderVelocity: Stream[Velocity] = Stream.periodic(period)(FeetPerSecondSquared(9.5)).integral
    val distanceFromAccelerometerLeft: Length = Feet(0.5) // calc = 2.5
    val distanceFromAccelerometerRight: Length = Feet(1) // calc = 10
    val allowedAccelerationDeviation: Acceleration = MetersPerSecondSquared(1)

    val isSlipping = Slipping.slippingDetection(angularVelocity,
      linearAcceleration,
      leftEncoderVelocity,
      rightEncoderVelocity,
      distanceFromAccelerometerLeft,
      distanceFromAccelerometerRight,
      allowedAccelerationDeviation)

    isSlipping.foreach(x => assert(x._1 && !x._2))

    triggerClock.apply(period)
    triggerClock.apply(period)
    triggerClock.apply(period)
    triggerClock.apply(period)
    triggerClock.apply(period)
    triggerClock.apply(period)
  }
}
