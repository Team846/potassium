package com.lynbrookrobotics.potassium.commons.drivetrain

import com.lynbrookrobotics.potassium.control.PIDConfig
import com.lynbrookrobotics.potassium.units.GenericValue._
import com.lynbrookrobotics.potassium.units._
import com.lynbrookrobotics.potassium.streams.{Periodic, Stream}
import com.lynbrookrobotics.potassium.{ClockMocking, Signal}
import squants.motion._
import squants.space.{Degrees, Feet, Meters}
import squants.time.{Milliseconds, Seconds}
import squants.{Acceleration, Angle, Dimensionless, Length, Percent, Velocity}
import org.scalatest.FunSuite

class PurePursuitControllerTests extends FunSuite {
  implicit val props = Signal.constant(new UnicycleProperties {
    override val maxForwardVelocity: Velocity = FeetPerSecond(10)
    override val maxTurnVelocity: AngularVelocity = RadiansPerSecond(10)
    override val maxAcceleration: Acceleration = FeetPerSecondSquared(15)
    override val defaultLookAheadDistance: Length = Feet(1)

    override val forwardControlGains = PIDConfig(
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

  private class TestDrivetrain extends UnicycleDrive with Drive{
    override type DriveSignal = UnicycleSignal
    override type DriveVelocity = UnicycleSignal

    override type Hardware = UnicycleHardware
    override type Properties = UnicycleProperties

    override protected def convertUnicycleToDrive(uni: UnicycleSignal): DriveSignal = uni

    override protected def controlMode(implicit hardware: Hardware,
                                       props: Properties): UnicycleControlMode = NoOperation

    override protected def driveClosedLoop(signal: Stream[DriveSignal])
                                          (implicit hardware: Hardware,
                                           props: Signal[Properties]): Stream[DriveSignal] =
      signal

    override type Drivetrain = Nothing
  }


  val origin = Point.origin
  val period = Milliseconds(5)
  implicit val (clock, triggerClock) = ClockMocking.mockedClockTicker

  test("Test if facing target while at start results in driving straight") {
    implicit val testDrivetrain = new TestDrivetrain
    implicit val hardware = new UnicycleHardware {
      override val forwardVelocity: Stream[Velocity] = Stream.periodic(period)(MetersPerSecond(0))
      override val turnVelocity: Stream[AngularVelocity] = Stream.periodic(period)(DegreesPerSecond(0))

      override val forwardPosition: Stream[Length] = null
      override val turnPosition: Stream[Angle] = Stream.periodic(period)(Degrees(90))
    }

    val controllers = testDrivetrain.UnicycleControllers

    val target = new Point(Feet(0), Feet(1))

    val path = hardware.turnPosition.mapToConstant[(Segment, Option[Segment])](
      (Segment(origin, target), None)
    )

    val position = hardware.turnPosition.mapToConstant(origin)

    val output = controllers.purePursuitControllerTurn(
      hardware.turnPosition.mapToConstant(Degrees(0)),
      position,
      path)._1

    var lastOutput = Percent(-10)
    output.foreach(lastOutput = _)

    triggerClock.apply(period)
    triggerClock.apply(period)

    assert(lastOutput == Percent(0), "Output is: " + lastOutput)
  }

  test("Test if 90 degrees from target turn output >= 100%") {
    val testDrivetrain = new TestDrivetrain
    implicit val hardware = new UnicycleHardware {
      override val forwardVelocity: Stream[Velocity] = Stream.periodic(period)(MetersPerSecond(0))
      override val turnVelocity: Stream[AngularVelocity] = Stream.periodic(period)(DegreesPerSecond(0))

      override val forwardPosition: Stream[Length] = null
      override val turnPosition: Stream[Angle] = Stream.periodic(period)(Degrees(0))
    }

    val controllers = testDrivetrain.UnicycleControllers

    val target = Point(Feet(0), Feet(1))

    val path: Stream[(Segment, Option[Segment])] =
      hardware.turnPosition.mapToConstant((Segment(origin, target), None))

    val position = hardware.turnPosition.mapToConstant(Point(Feet(1), Feet(1)))

    val output = controllers.purePursuitControllerTurn(
      hardware.turnPosition.mapToConstant(Degrees(0)),
      position,
      path
    )._1

    var out = Percent(-10)

    output.foreach(out = _)
    triggerClock.apply(period)
    triggerClock.apply(period)

    assert(out.abs >= Percent(100))
  }

  test("Test if when -5 degrees from target, turn output is -50%") {
    val testDrivetrain = new TestDrivetrain
    implicit val hardware = new UnicycleHardware {
      override val forwardVelocity: Stream[Velocity] = Stream.periodic(period)(MetersPerSecond(0))
      override val turnVelocity: Stream[AngularVelocity] = Stream.periodic(period)(DegreesPerSecond(0))

      override val forwardPosition: Stream[Length] = null
      override val turnPosition: Stream[Angle] = Stream.periodic(period)(Degrees(0))
    }
    val controllers = testDrivetrain.UnicycleControllers

    implicit val props = Signal.constant(new UnicycleProperties {
      override val maxForwardVelocity: Velocity = FeetPerSecond(10)
      override val maxTurnVelocity: AngularVelocity = RadiansPerSecond(10)
      override val maxAcceleration: Acceleration = FeetPerSecondSquared(15)
      override val defaultLookAheadDistance: Length = Feet(1)

      override val forwardControlGains = PIDConfig(
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

    val target = Point(Feet(0), Feet(1))
    val position = hardware.turnPosition.mapToConstant(Point(Feet(1), Feet(1)))

    val path: Stream[(Segment, Option[Segment])] =
      hardware.turnPosition.mapToConstant((Segment(origin, target), None))

    val output = controllers.purePursuitControllerTurn(
      hardware.turnPosition.mapToConstant(Degrees(-85)),
      position,
      path
    )._1

    var out = Percent(-10)
    output.foreach(out = _)
    triggerClock.apply(Milliseconds(5))
    triggerClock.apply(Milliseconds(5))

    implicit val Tolerance = Percent(0.001)
    assert(out ~= Percent(-50), s"result is ${out.toPercent}")
  }

  test("Test if overshooting target results not making full turn"){
    val testDrivetrain = new TestDrivetrain
    implicit val hardware = new UnicycleHardware {
      override val forwardVelocity: Stream[Velocity] = Stream.periodic(period)(MetersPerSecond(0))
      override val turnVelocity: Stream[AngularVelocity] = Stream.periodic(period)(DegreesPerSecond(0))

      override val forwardPosition: Stream[Length] = null
      override val turnPosition: Stream[Angle] = Stream.periodic(period)(Degrees(45))
    }
    val controllers = testDrivetrain.UnicycleControllers

    val target = Point(Feet(1), Feet(1))
    val position = hardware.turnPosition.mapToConstant(Point(Feet(2), Feet(2)))
    val path: Stream[(Segment, Option[Segment])] = hardware.turnPosition.mapToConstant((Segment(origin, target), None))

    val output = controllers.purePursuitControllerTurn(
      hardware.turnPosition.mapToConstant(Degrees(45)),
      position,
      path
    )._1

    var out = Percent(-10)
    output.foreach(out = _)

    triggerClock.apply(period)
    triggerClock.apply(period)

    implicit val Tolerance = Percent(0.001)
    assert(out ~= Percent(0))
  }
}
