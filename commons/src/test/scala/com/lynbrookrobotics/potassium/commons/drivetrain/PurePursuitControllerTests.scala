package com.lynbrookrobotics.potassium.commons.drivetrain

import com.lynbrookrobotics.potassium.control.PIDConfig
import com.lynbrookrobotics.potassium.units.GenericValue._
import com.lynbrookrobotics.potassium.units._
import com.lynbrookrobotics.potassium.{PeriodicSignal, Signal, SignalLike}
import squants.motion._
import squants.space.{Degrees, Feet, Meters}
import squants.time.{Milliseconds, Seconds}
import squants.{Acceleration, Length, Percent, Velocity}
import org.scalatest.FunSuite

class PurePursuitControllerTests extends FunSuite {
  implicit val props = Signal.constant(new UnicycleProperties {
    override val maxForwardVelocity: Velocity = FeetPerSecond(10)
    override val maxTurnVelocity: AngularVelocity = RadiansPerSecond(10)
    override val maxAcceleration: Acceleration = FeetPerSecondSquared(15)
    override val defaultLookAheadDistance: Length = Feet(1)

    override val forwardControlGains = PIDConfig( // Why is this being used?
      Percent(0) / MetersPerSecond(1),
      Percent(0) / Meters(1),
      Percent(0) / MetersPerSecondSquared(1)
    )

    override val turnControlGains = PIDConfig(
      Percent(10) / DegreesPerSecond(1),
      Percent(0) / Degrees(1),
      Percent(0) / (DegreesPerSecond(1).toGeneric / Seconds(1))
    )

    override val forwardPositionControlGains = PIDConfig(
      Percent(100) / Meters(10),
      Percent(0) / (Meters(1).toGeneric * Seconds(1)),
      Percent(0) / MetersPerSecond(1)
    )

    override val turnPositionControlGains = PIDConfig(
      Percent(100) / Degrees(10),
      Percent(0) / (Degrees(1).toGeneric * Seconds(1)),
      Percent(0) / DegreesPerSecond(1)
    )
  })

  private class TestDrivetrain extends UnicycleDrive {
    override type DriveSignal = UnicycleSignal
    override type DriveVelocity = UnicycleSignal

    override type Hardware = UnicycleHardware
    override type Properties = UnicycleProperties

    override protected def convertUnicycleToDrive(uni: UnicycleSignal): DriveSignal = uni

    override protected def controlMode(implicit hardware: Hardware,
                                       props: Properties): UnicycleControlMode = NoOperation

    override protected def driveClosedLoop(signal: SignalLike[DriveSignal])
                                          (implicit hardware: Hardware,
                                           props: Signal[Properties]): PeriodicSignal[DriveSignal] =
      signal.toPeriodic

    override type Drivetrain = Nothing
  }

  val origin = new Point(Feet(0), Feet(0))


  test("Test if facing target while at start results in driving straight") {
    val testDrivetrain = new TestDrivetrain
    val controllers = testDrivetrain.UnicycleControllers

    val target = new Point(Feet(1), Feet(0))
    val path = (Segment(origin, target), None)

    val position = Signal.constant(origin).toPeriodic

    val lookAheadPoint = position.zip(props).map { p =>
      val (pose, props) = p
      controllers.getLookAheadPoint(
        path,
        pose,
        props.defaultLookAheadDistance)
    }

    val output = controllers.purePursuitControllerTurn(
      Signal.constant(Degrees(90)),
      position,
      lookAheadPoint,
      false
    )._1

    val currOutput = output.currentValue(Milliseconds(5))
    assert(currOutput == Percent(0), "Output is: " + currOutput)
  }

  test("Test if 90 degrees from target turn output >= 100%") {
    val testDrivetrain = new TestDrivetrain
    val controllers = testDrivetrain.UnicycleControllers

    val target = new Point(Feet(0), Feet(1))
    val path = (Segment(origin, target), None)

    val position = Signal.constant(new Point(Feet(1), Feet(1))).toPeriodic

    val lookAheadPoint = position.zip(props).map { p =>
      val (pose, props) = p
      controllers.getLookAheadPoint(
        path,
        pose,
        props.defaultLookAheadDistance)
    }

    val output = controllers.purePursuitControllerTurn(
      Signal.constant(Degrees(0)),
      position,
      lookAheadPoint,
      false
    )._1

    assert(output.currentValue(Milliseconds(5)).abs >= Percent(100))
  }

  test("Test if when -5 degrees from target, turn output is -50%") {
    val testDrivetrain = new TestDrivetrain
    val controllers = testDrivetrain.UnicycleControllers

    implicit val props = Signal.constant(new UnicycleProperties {
      override val maxForwardVelocity: Velocity = FeetPerSecond(10)
      override val maxTurnVelocity: AngularVelocity = RadiansPerSecond(10)
      override val maxAcceleration: Acceleration = FeetPerSecondSquared(15)
      override val defaultLookAheadDistance: Length = Feet(1)

      override val forwardControlGains = PIDConfig( // Why is this being used?
        Percent(0) / MetersPerSecond(1),
        Percent(0) / Meters(1),
        Percent(0) / MetersPerSecondSquared(1)
      )

      override val turnControlGains = PIDConfig(
        Percent(10) / DegreesPerSecond(1),
        Percent(0) / Degrees(1),
        Percent(0) / (DegreesPerSecond(1).toGeneric / Seconds(1))
      )

      override val forwardPositionControlGains = PIDConfig(
        Percent(100) / Meters(10),
        Percent(0) / (Meters(1).toGeneric * Seconds(1)),
        Percent(0) / MetersPerSecond(1)
      )

      override val turnPositionControlGains = PIDConfig(
        Percent(100) / Degrees(10),
        Percent(0) / (Degrees(1).toGeneric * Seconds(1)),
        Percent(0) / DegreesPerSecond(1)
      )
    })

    val target = new Point(Feet(0), Feet(1))
    val position = Signal.constant(new Point(Feet(1), Feet(1))).toPeriodic
    val path = (Segment(origin, target), None)


    val lookAheadPoint = position.zip(props).map { p =>
      val (pose, props) = p
      controllers.getLookAheadPoint(
        path,
        pose,
        props.defaultLookAheadDistance)
    }

    val output = controllers.purePursuitControllerTurn(
      Signal.constant(Degrees(-85)),
      position,
      lookAheadPoint,
      false
    )._1

    implicit val Tolerance = Percent(0.001)
    val result = output.currentValue(Milliseconds(5))
    assert(result ~= Percent(-50))
  }

  test("Test if overshooting target results not making full turn"){
    val testDrivetrain = new TestDrivetrain
    val controllers = testDrivetrain.UnicycleControllers

    val target = new Point(Feet(1), Feet(1))
    val position = Signal.constant(new Point(Feet(2), Feet(2))).toPeriodic
    val path = (Segment(origin, target), None)


    val lookAheadPoint = position.zip(props).map { p =>
      val (pose, props) = p
      controllers.getLookAheadPoint(
        path,
        pose,
        props.defaultLookAheadDistance)
    }

    val output = controllers.purePursuitControllerTurn(
      Signal.constant(Degrees(45)),
      position,
      lookAheadPoint,
      false
    )._1

    implicit val Tolerance = Percent(0.001)
    val result = output.currentValue(Milliseconds(5))
    assert(result ~= Percent(0))
  }
}
