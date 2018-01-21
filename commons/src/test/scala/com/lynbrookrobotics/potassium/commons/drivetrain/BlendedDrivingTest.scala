package com.lynbrookrobotics.potassium.commons.drivetrain

import com.lynbrookrobotics.potassium.commons.drivetrain.twoSided.{BlendedDriving, TwoSidedDriveProperties, TwoSidedVelocity}
import com.lynbrookrobotics.potassium.control.PIDConfig
import com.lynbrookrobotics.potassium.streams.{Periodic, Stream}
import com.lynbrookrobotics.potassium.units.GenericValue._
import com.lynbrookrobotics.potassium.units.{Ratio, _}
import com.lynbrookrobotics.potassium.{ClockMocking, Signal}
import org.scalatest.FunSuite
import squants.motion.{AngularVelocity, DegreesPerSecond, MetersPerSecond, MetersPerSecondSquared, _}
import squants.space._
import squants.time.{Milliseconds, Seconds}
import squants.{Acceleration, Each, Length, Percent, Velocity}

class BlendedDrivingTest extends FunSuite {
  implicit val props = Signal.constant(new TwoSidedDriveProperties {
    lazy override val maxForwardVelocity: Velocity = MetersPerSecond(10)
    override val maxTurnVelocity: AngularVelocity = DegreesPerSecond(10)
    override val maxAcceleration: Acceleration = FeetPerSecondSquared(10)
    override val defaultLookAheadDistance: Length = Feet(0.5)

    override val forwardControlGains = PIDConfig(
      Percent(0) / MetersPerSecond(1),
      Percent(0) / Meters(1),
      Percent(0) / MetersPerSecondSquared(1)
    )

    override val turnControlGains = PIDConfig(
      Percent(0) / DegreesPerSecond(1),
      Percent(0) / Degrees(1),
      Percent(0) / (DegreesPerSecond(1).toGeneric / Seconds(1))
    )

    override val forwardPositionControlGains = PIDConfig(
      Percent(100) / defaultLookAheadDistance,
      Percent(0) / (Meters(1).toGeneric * Seconds(1)),
      Percent(0) / MetersPerSecond(1)
    )

    override val turnPositionControlGains = PIDConfig(
      Percent(100) / Degrees(90),
      Percent(0) / (Degrees(1).toGeneric * Seconds(1)),
      Percent(0) / DegreesPerSecond(1)
    )

    override val maxLeftVelocity: Velocity = FeetPerSecond(10)
    override val maxRightVelocity: Velocity = FeetPerSecond(10)
    override val leftControlGains: ForwardVelocityGains = PIDConfig(
      Percent(100) / FeetPerSecond(90),
      Percent(0) / Feet(1),
      Percent(0) / FeetPerSecondSquared(1)
    )
    override val rightControlGains: ForwardVelocityGains = PIDConfig(
      Percent(100) / FeetPerSecond(90),
      Percent(0) / Feet(1),
      Percent(0) / FeetPerSecondSquared(1)
    )
    override val track: Length = Inches(21)
    override val blendExponent: Double = 0.5
  })

  test("drive at a radius of 1 foot") {
    var lastBlendedSpeed: TwoSidedVelocity = null

    val (radiuses, publishRadius) = Stream.manual[Length]
    val (velocities, publishVelocity) = Stream.manual[Velocity]
    val circularMotions = BlendedDriving.circularMotion(velocities, radiuses)
    circularMotions.foreach(lastBlendedSpeed = _)

    publishRadius(-props.get.track / 2)
    publishVelocity(FeetPerSecond(1))

    println(s"last value was  $lastBlendedSpeed")
    assert(lastBlendedSpeed.left.toFeetPerSecond == 0 && lastBlendedSpeed.right.toFeetPerSecond == 2, s"was ${lastBlendedSpeed}")
  }

  test("blendedDriving with tank speed: 5ft/s, velocity: 5 ft/s, curvatures") {
    implicit val (clock, clockTrigger) = ClockMocking.mockedClockTicker

    val tankSpeeds = Stream.periodic(Milliseconds(5)) {
      TwoSidedVelocity(FeetPerSecond(5), FeetPerSecond(-4))
    }

    val targetForwards = Stream.periodic(Milliseconds(5)) {
      FeetPerSecond(5)
    }
    val curvatures = Stream.periodic(Milliseconds(5)) {
      Ratio(Each(10), Feet(1))
    }
    var lastBlendedSpeed: TwoSidedVelocity = TwoSidedVelocity(FeetPerSecond(0), FeetPerSecond(0))
    clockTrigger(Milliseconds(5))
    BlendedDriving.blendedDrive(tankSpeeds, targetForwards, curvatures).foreach(lastBlendedSpeed = _)
    assert(lastBlendedSpeed.left.value > 0)
  }
}
