package com.lynbrookrobotics.potassium.commons.drivetrain

import com.lynbrookrobotics.potassium.ClockMocking
import com.lynbrookrobotics.potassium.streams.Stream
import com.lynbrookrobotics.potassium.commons.position.Slipping
import org.scalatest.FunSuite
import squants.motion._
import squants.space.{Feet, Length}
import squants.time.Milliseconds

class SlippingTest extends FunSuite {


  val period = Milliseconds(1000)

  test("Test that if robot is not turning and moving there is no slipping") {
    implicit val (clock, triggerClock) = ClockMocking.mockedClockTicker
    val angularVelocity: Stream[AngularVelocity] = Stream.periodic(period)(RadiansPerSecondSquared(0)).integral
    val linearAcceleration: Stream[Acceleration] = Stream.periodic(period)(FeetPerSecondSquared(0))
    val leftEncoderVelocity: Stream[Velocity] = Stream.periodic(period)(FeetPerSecondSquared(0)).integral
    val rightEncoderVelocity: Stream[Velocity] = Stream.periodic(period)(FeetPerSecondSquared(0)).integral
    val distanceFromAccelerometerLeft: Length = Feet(1)
    val distanceFromAccelerometerRight: Length = Feet(1)
    val allowedAccelerationDeviation: Acceleration = MetersPerSecondSquared(0.1)

    var updated = false
    val isSlipping = Slipping.slippingDetection(angularVelocity,
      linearAcceleration,
      leftEncoderVelocity,
      rightEncoderVelocity,
      distanceFromAccelerometerLeft,
      distanceFromAccelerometerRight,
      allowedAccelerationDeviation)
    isSlipping.foreach { x =>
      updated = true
      assert(!x._1 && !x._2)
    }

    triggerClock.apply(period)
    triggerClock.apply(period)
    triggerClock.apply(period)

    assert(updated)

  }

  test("Test that if robot is not turning there is no slipping") {
    implicit val (clock, triggerClock) = ClockMocking.mockedClockTicker
    val angularVelocity: Stream[AngularVelocity] = Stream.periodic(period)(RadiansPerSecondSquared(0)).integral
    val linearAcceleration: Stream[Acceleration] = Stream.periodic(period)(FeetPerSecondSquared(1))
    val leftEncoderVelocity: Stream[Velocity] = Stream.periodic(period)(FeetPerSecondSquared(1)).integral
    val rightEncoderVelocity: Stream[Velocity] = Stream.periodic(period)(FeetPerSecondSquared(1)).integral
    val distanceFromAccelerometerLeft: Length = Feet(1)
    val distanceFromAccelerometerRight: Length = Feet(1)
    val allowedAccelerationDeviation: Acceleration = MetersPerSecondSquared(0.1)

    val isSlipping = Slipping.slippingDetection(angularVelocity,
      linearAcceleration,
      leftEncoderVelocity,
      rightEncoderVelocity,
      distanceFromAccelerometerLeft,
      distanceFromAccelerometerRight,
      allowedAccelerationDeviation)
    isSlipping.foreach {
      x => assert(!x._1 && !x._2)
    }

    triggerClock.apply(period)
    triggerClock.apply(period)
    triggerClock.apply(period)

  }

  test("Test that robot detects slipping when right and left wheel turn at different rates") {
    implicit val (clock, triggerClock) = ClockMocking.mockedClockTicker
    val angularVelocity: Stream[AngularVelocity] = Stream.periodic(period)(RadiansPerSecondSquared(1)).integral
    val linearAcceleration: Stream[Acceleration] = Stream.periodic(period)(FeetPerSecondSquared(1))
    val leftEncoderVelocity: Stream[Velocity] = Stream.periodic(period)(FeetPerSecondSquared(0)).integral
    val rightEncoderVelocity: Stream[Velocity] = Stream.periodic(period)(FeetPerSecondSquared(5)).integral
    val distanceFromAccelerometerLeft: Length = Feet(1)
    val distanceFromAccelerometerRight: Length = Feet(1)
    val allowedAccelerationDeviation: Acceleration = MetersPerSecondSquared(0.1)

    val isSlipping = Slipping.slippingDetection(angularVelocity,
      linearAcceleration,
      leftEncoderVelocity,
      rightEncoderVelocity,
      distanceFromAccelerometerLeft,
      distanceFromAccelerometerRight,
      allowedAccelerationDeviation)
    isSlipping.foreach {
      x => assert(!x._1 && x._2)
    }

    triggerClock.apply(period)
    triggerClock.apply(period)
    triggerClock.apply(period)

  }

  test("Test that robot is slipping if it is not moving but the left and right encoders say it is") {
    implicit val (clock, triggerClock) = ClockMocking.mockedClockTicker

    //is not slipping
    val checkNotSlipping = Slipping.slippingDetection(
      angularVelocity = Stream.periodic(period)(RadiansPerSecondSquared(0)).integral,
      linearAcceleration = Stream.periodic(period)(FeetPerSecondSquared(0)),
      leftEncoderVelocity = Stream.periodic(period)(FeetPerSecondSquared(1)).integral,
      rightEncoderVelocity = Stream.periodic(period)(FeetPerSecondSquared(1)).integral,
      distanceFromAccelerometerLeft = Feet(0),
      distanceFromAccelerometerRight = Feet(0),
      allowedAccelerationDeviation = MetersPerSecondSquared(0.1))

    checkNotSlipping.foreach(x => assert(x._1 && x._2))

    triggerClock.apply(period)
    triggerClock.apply(period)
    triggerClock.apply(period)

  }

  test("Test that robot is not slipping") {
    implicit val (clock, triggerClock) = ClockMocking.mockedClockTicker

    //is not slipping
    val checkNotSlipping = Slipping.slippingDetection(
      angularVelocity = Stream.periodic(period)(RadiansPerSecondSquared(1)).integral,
      linearAcceleration = Stream.periodic(period)(FeetPerSecondSquared(1)),
      leftEncoderVelocity = Stream.periodic(period)(FeetPerSecondSquared(1)).integral,
      rightEncoderVelocity = Stream.periodic(period)(FeetPerSecondSquared(1)).integral,
      distanceFromAccelerometerLeft = Feet(0),
      distanceFromAccelerometerRight = Feet(0),
      allowedAccelerationDeviation = MetersPerSecondSquared(0.1))

    checkNotSlipping.foreach(x => assert(!x._1 && !x._2))

    triggerClock.apply(period)
    triggerClock.apply(period)
    triggerClock.apply(period)

  }

}
