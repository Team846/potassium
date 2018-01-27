package com.lynbrookrobotics.potassium.commons.drivetrain

import com.lynbrookrobotics.potassium.Signal
import com.lynbrookrobotics.potassium.commons.drivetrain.twoSided.{BlendedDriving, TwoSidedDriveProperties, TwoSidedVelocity}
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
  def generateProps(blendExp: Double, maxForward: Velocity = MetersPerSecond(10)): Signal[TwoSidedDriveProperties] = {
    Signal.constant(new TwoSidedDriveProperties {
      lazy override val maxForwardVelocity: Velocity = maxForward
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
      override val track: Length = Feet(1)
      override val blendExponent: Double = blendExp
    })
  }

  implicit val props: Signal[TwoSidedDriveProperties] = generateProps(0.5)

  test("drive at a radius of 1 foot") {
    var lastBlendedSpeed: TwoSidedVelocity = null

    val (radiuses, publishRadius) = Stream.manual[Length]
    val (velocities, publishVelocity) = Stream.manual[Velocity]
    val circularMotions = BlendedDriving.circularMotion(velocities, radiuses)
    circularMotions.foreach(lastBlendedSpeed = _)

    publishRadius(-props.get.track / 2)
    publishVelocity(FeetPerSecond(1))

    assert(lastBlendedSpeed.left.toFeetPerSecond == 0 && lastBlendedSpeed.right.toFeetPerSecond == 2)

    publishRadius(Feet(1))
    publishVelocity(FeetPerSecond(2))

    assert(lastBlendedSpeed.right.toFeetPerSecond == 1 && lastBlendedSpeed.left.toFeetPerSecond == 3)
  }

  def runBlendedDrive(props: Signal[TwoSidedDriveProperties]): Velocity = {
    BlendedDriving.blend(FeetPerSecond(0), FeetPerSecond(1), FeetPerSecond(1))(props)
  }

  test("Test blend function") {
    var propsWithBlend = generateProps(0)
    assert(runBlendedDrive(propsWithBlend) == FeetPerSecond(0))

    propsWithBlend = generateProps(1, FeetPerSecond(2))
    assert(runBlendedDrive(propsWithBlend) == FeetPerSecond(0.5))

    propsWithBlend = generateProps(0.5, FeetPerSecond(4))
    assert(runBlendedDrive(propsWithBlend) == FeetPerSecond(0.5))
  }

  test("blendedDriving with infinity radius & negative velocity") {
    val (tankSpeeds, publishTankSpeed) = Stream.manual[TwoSidedVelocity]
    val (targetForwards, publishTargetForwards) = Stream.manual[Velocity]
    val (curvatures, publishCurvatures) = Stream.manual[Ratio[Dimensionless, Length]]
    var lastBlendedSpeed: TwoSidedVelocity = null

    val props = generateProps(0)
    BlendedDriving.blendedDrive(tankSpeeds, targetForwards, curvatures)(props).foreach(lastBlendedSpeed = _)

    publishTankSpeed(TwoSidedVelocity(FeetPerSecond(0), FeetPerSecond(0)))
    publishTargetForwards(FeetPerSecond(-1))
    publishCurvatures(Ratio(Each(0), Feet(1)))

    assert(lastBlendedSpeed.left.value < 0 && lastBlendedSpeed.right.value < 0)

    publishTankSpeed(TwoSidedVelocity(FeetPerSecond(0), FeetPerSecond(0)))
    publishTargetForwards(FeetPerSecond(-2))
    publishCurvatures(Ratio(Each(0), Feet(1)))

    assert(lastBlendedSpeed.left.value < 0 && lastBlendedSpeed.right.value < 0)
  }

  test("Steering angles at forward and negative velocities") {
    val (velocityStream, publishVelocity) = Stream.manual[Velocity]
    val (curvatureStream, publishCurvature) = Stream.manual[Ratio[Dimensionless, Length]]


  }
}
