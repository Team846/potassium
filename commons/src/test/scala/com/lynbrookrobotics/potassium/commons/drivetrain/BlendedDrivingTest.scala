package com.lynbrookrobotics.potassium.commons.drivetrain

import com.lynbrookrobotics.potassium.Signal
import com.lynbrookrobotics.potassium.commons.drivetrain.twoSided.{BlendedDriving, TwoSided, TwoSidedDriveProperties}
import com.lynbrookrobotics.potassium.control.PIDConfig
import com.lynbrookrobotics.potassium.streams.Stream
import com.lynbrookrobotics.potassium.units.GenericValue._
import com.lynbrookrobotics.potassium.units._
import org.scalatest.FunSuite
import squants.motion.{AngularVelocity, DegreesPerSecond, MetersPerSecond, MetersPerSecondSquared, _}
import squants.space._
import squants.time.Seconds
import squants.{Acceleration, Dimensionless, Each, Length, Percent, Velocity}

class BlendedDrivingTest extends FunSuite {
  def generateProps(blendExp: Double,
                    maxForward: Velocity = MetersPerSecond(10)): Signal[TwoSidedDriveProperties] = {
    Signal.constant(new TwoSidedDriveProperties {
      lazy override val maxForwardVelocity: Velocity = maxForward
      override val maxTurnVelocity: AngularVelocity = DegreesPerSecond(10)
      override val maxAcceleration: Acceleration = FeetPerSecondSquared(10)
      override val defaultLookAheadDistance: Length = Feet(0.5)

      override val forwardVelocityGains: ForwardVelocityGains = PIDConfig(
        Percent(0) / MetersPerSecond(1),
        Percent(0) / Meters(1),
        Percent(0) / MetersPerSecondSquared(1)
      )

      override val turnVelocityGains: TurnVelocityGains = PIDConfig(
        Percent(0) / DegreesPerSecond(1),
        Percent(0) / Degrees(1),
        Percent(0) / (DegreesPerSecond(1).toGeneric / Seconds(1))
      )

      override val forwardPositionGains: ForwardPositionGains = PIDConfig(
        Percent(100) / defaultLookAheadDistance,
        Percent(0) / (Meters(1).toGeneric * Seconds(1)),
        Percent(0) / MetersPerSecond(1)
      )

      override val turnPositionGains: TurnPositionGains = PIDConfig(
        Percent(100) / Degrees(90),
        Percent(0) / (Degrees(1).toGeneric * Seconds(1)),
        Percent(0) / DegreesPerSecond(1)
      )

      override val maxLeftVelocity: Velocity = FeetPerSecond(10)
      override val maxRightVelocity: Velocity = FeetPerSecond(10)
      override val leftVelocityGains: ForwardVelocityGains = PIDConfig(
        Percent(100) / FeetPerSecond(90),
        Percent(0) / Feet(1),
        Percent(0) / FeetPerSecondSquared(1)
      )
      override val rightVelocityGains: ForwardVelocityGains = PIDConfig(
        Percent(100) / FeetPerSecond(90),
        Percent(0) / Feet(1),
        Percent(0) / FeetPerSecondSquared(1)
      )
      override val track: Length = Feet(1)
      override val blendExponent: Double = blendExp
    })
  }


  test("Test driving at a radius of 1 foot and -track / 2") {
    implicit val props: Signal[TwoSidedDriveProperties] = generateProps(blendExp = 0.5)

    var lastBlendedSpeed: TwoSided[Velocity] = null

    val (radius, publishRadius) = Stream.manual[Length]
    val (velocity, publishVelocity) = Stream.manual[Velocity]
    val circularMotion = BlendedDriving.driveWithRadius(radius, velocity)
    circularMotion.foreach(lastBlendedSpeed = _)

    publishRadius(-props.get.track / 2)
    publishVelocity(FeetPerSecond(1))

    assert(lastBlendedSpeed.left.toFeetPerSecond == 0 && lastBlendedSpeed.right.toFeetPerSecond == 2)

    publishRadius(Feet(1))
    publishVelocity(FeetPerSecond(2))

    assert(lastBlendedSpeed.right.toFeetPerSecond == 1 && lastBlendedSpeed.left.toFeetPerSecond == 3)
  }

  test("Test blend function produces correct weighted averages") {
    assert(
      BlendedDriving.blend(
        FeetPerSecond(0),
        FeetPerSecond(1),
        FeetPerSecond(1))(generateProps(blendExp = 0)) == FeetPerSecond(0))

    assert(
      BlendedDriving.blend(
        FeetPerSecond(0),
        FeetPerSecond(1),
        FeetPerSecond(1))(generateProps(blendExp = 1, FeetPerSecond(2))) == FeetPerSecond(0.5))

    assert(
      BlendedDriving.blend(
        FeetPerSecond(0),
        FeetPerSecond(1),
        FeetPerSecond(1))(generateProps(blendExp = 0.5, FeetPerSecond(4))) == FeetPerSecond(0.5))
  }

  test("blendedDriving with infinity radius & negative velocity results in straight backwards motion") {
    val (tankSpeed, publishTankSpeed) = Stream.manual[TwoSided[Velocity]]
    val (targetForwardVelocity, publishTargetForward) = Stream.manual[Velocity]
    val (curvature, publishCurvature) = Stream.manual[Ratio[Dimensionless, Length]]

    var lastBlendedSpeed: TwoSided[Velocity] = null

    implicit val props = generateProps(blendExp = 0)
    BlendedDriving.blendedDrive(
      tankSpeed,
      targetForwardVelocity,
      curvature).foreach(lastBlendedSpeed = _)

    publishTankSpeed(TwoSided(FeetPerSecond(0), FeetPerSecond(0)))
    publishTargetForward(FeetPerSecond(-1))
    publishCurvature(Ratio(Each(0), Feet(1)))

    assert(lastBlendedSpeed.left < FeetPerSecond(0) && lastBlendedSpeed.right < FeetPerSecond(0))

    publishTankSpeed(TwoSided(FeetPerSecond(0), FeetPerSecond(0)))
    publishTargetForward(FeetPerSecond(-2))
    publishCurvature(Ratio(Each(0), Feet(1)))

    assert(lastBlendedSpeed.left < FeetPerSecond(0) && lastBlendedSpeed.right < FeetPerSecond(0))
  }
}
