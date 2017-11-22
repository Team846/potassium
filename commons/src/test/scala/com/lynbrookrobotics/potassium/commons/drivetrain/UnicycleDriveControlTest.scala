package com.lynbrookrobotics.potassium.commons.drivetrain

import com.lynbrookrobotics.potassium.streams.Stream
import com.lynbrookrobotics.potassium.control.{PIDConfig, PIDFConfig, TrapezoidalProfileConfig}
import com.lynbrookrobotics.potassium.units.GenericValue._
import com.lynbrookrobotics.potassium.units._
import com.lynbrookrobotics.potassium.{ClockMocking, Signal}
import org.scalacheck.{Arbitrary, Gen}
import squants.motion._
import squants.space.{Degrees, Meters}
import squants.time.{Milliseconds, Seconds}
import squants.{Acceleration, Angle, Dimensionless, Each, Length, Percent, Velocity}
import org.scalacheck.Prop.forAll
import org.scalatest.FunSuite
import org.scalatest.prop.Checkers._

class UnicycleDriveControlTest extends FunSuite {
  val period = Milliseconds(5)
  implicit val (clock, trigggerClock) = ClockMocking.mockedClockTicker

  implicit val hardware: UnicycleHardware = new UnicycleHardware {


    override val forwardVelocity: Stream[Velocity] = Stream.periodic(period)(MetersPerSecond(0))(clock)
    override val turnVelocity: Stream[AngularVelocity] = Stream.periodic(period)(DegreesPerSecond(0))(clock)

    override val forwardPosition: Stream[Length] = null
    override val turnPosition: Stream[Angle] = null
  }

  private class TestDrivetrain extends UnicycleDrive {
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

  implicit val arbitraryVelocity: Arbitrary[Velocity] = Arbitrary(
    Gen.chooseNum[Double](-100D, 100D).map(d => MetersPerSecond(d))
  )

  implicit val arbitraryAngularVelocity: Arbitrary[AngularVelocity] = Arbitrary(
    Gen.chooseNum[Double](-100D, 100D).map(d => DegreesPerSecond(d))
  )

  test("Open forward loop produces same forward speed as input and zero turn speed") {
    val drive = new TestDrivetrain

    check(forAll { x: Double =>
      val out = drive.UnicycleControllers.openForwardOpenDrive(
        hardware.forwardVelocity.mapToConstant(Each(x)))

      var forward = Percent(-10)
      var turn = Percent(-10)
      out.foreach(o => {
        forward = o.forward
        turn = o.turn
      })

      trigggerClock.apply(period)

      forward.toEach == x && turn.toEach == 0
    })
  }

  test("Open turn loop produces same turn speed as input and zero forward speed") {
    val drive = new TestDrivetrain

    check(forAll { x: Double =>
      val out = drive.UnicycleControllers.openTurnOpenDrive(hardware.turnVelocity.mapToConstant(Each(x)))

      var forward = Percent(-10)
      var turn = Percent(-10)
      out.foreach(o => {
        forward = o.forward
        turn = o.turn
      })

      trigggerClock.apply(period)

      turn.toEach == x && forward.toEach == 0
    })
  }

  test("Closed loop with only feed-forward is essentially open loop") {
    implicit val props = Signal.constant(new UnicycleProperties {
      override val maxForwardVelocity: Velocity = MetersPerSecond(10)
      override val maxTurnVelocity: AngularVelocity = DegreesPerSecond(10)
      override val maxAcceleration: Acceleration = FeetPerSecondSquared(10)
      override val defaultLookAheadDistance: Length = null

      override val trapezoidalProfileConfig: TrapezoidalProfileConfig[Velocity, Acceleration, Length] = null

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

      override val forwardPositionControlGains = null
      override val turnPositionControlGains = null
    })

    val drive = new TestDrivetrain

    check(forAll { (fwd: Velocity, turn: AngularVelocity) =>
      val in = hardware.turnVelocity.mapToConstant(UnicycleVelocity(fwd, turn))
      val out = drive.UnicycleControllers.velocityControl(in)

      var forwardOut = Percent(-10)
      var turnOut = Percent(-10)
      out.foreach(o => {
        forwardOut = o.forward
        turnOut = o.turn
      })

      trigggerClock.apply(period)
      trigggerClock.apply(period)

      (math.abs(forwardOut.toEach - (fwd.toMetersPerSecond / 10)) <= 0.01) &&
        (math.abs(turnOut.toEach - (turn.toDegreesPerSecond / 10)) <= 0.01)
    })
  }

  test("Forward position control when relative distance is zero returns zero speed") {
    val drive = new TestDrivetrain

    val props = Signal.constant(new UnicycleProperties {
      override val maxForwardVelocity: Velocity = null
      override val maxTurnVelocity: AngularVelocity = null
      override val maxAcceleration: Acceleration = null
      override val defaultLookAheadDistance: Length = null

      override val trapezoidalProfileConfig: TrapezoidalProfileConfig[Velocity, Acceleration, Length] = null

      override val forwardControlGains = null
      override val turnControlGains = null

      override val forwardPositionControlGains = PIDConfig(
        Percent(100) / Meters(1),
        Percent(0) / (Meters(1).toGeneric * Seconds(1)),
        Percent(0) / MetersPerSecond(1)
      )

      override val turnPositionControlGains = null
    })

    val hardware: UnicycleHardware = new UnicycleHardware {
      override val forwardVelocity: Stream[Velocity] = Stream.periodic(period)(MetersPerSecond(0))
      override val turnVelocity: Stream[AngularVelocity] = Stream.periodic(period)(DegreesPerSecond(0))

      override val forwardPosition: Stream[Length] = Stream.periodic(period)(Meters(5))
      override val turnPosition: Stream[Angle] = null
    }

    val out = drive.UnicycleControllers.
      forwardPositionControl(Meters(5))(hardware, props)._1

    var forwardOut = Percent(-10)
    var turnOut = Percent(-10)
    out.foreach(o => {
      forwardOut = o.forward
      turnOut = o.turn
    })

    trigggerClock.apply(period)
    trigggerClock.apply(period)

    assert(forwardOut.toPercent == 0)
  }

  test("Forward position control returns correct proportional control (forward)") {
    val drive = new TestDrivetrain

    val props = Signal.constant(new UnicycleProperties {
      override val maxForwardVelocity: Velocity = null
      override val maxTurnVelocity: AngularVelocity = null
      override val maxAcceleration: Acceleration = null
      override val defaultLookAheadDistance: Length = null

      override val forwardControlGains = null
      override val turnControlGains = null

      override val trapezoidalProfileConfig: TrapezoidalProfileConfig[Velocity, Acceleration, Length] = null

      override val forwardPositionControlGains = PIDConfig(
        Percent(100) / Meters(10),
        Percent(0) / (Meters(1).toGeneric * Seconds(1)),
        Percent(0) / MetersPerSecond(1)
      )

      override val turnPositionControlGains = null
    })

    val hardware: UnicycleHardware = new UnicycleHardware {
      override val forwardVelocity: Stream[Velocity] = Stream.periodic(period)(MetersPerSecond(0))
      override val turnVelocity: Stream[AngularVelocity] = Stream.periodic(period)(DegreesPerSecond(0))

      override val forwardPosition: Stream[Length] = Stream.periodic(period)(Meters(0))
      override val turnPosition: Stream[Angle] = null
    }

    val out = drive.UnicycleControllers.
      forwardPositionControl(Meters(5))(hardware, props)._1

    var forwardOut = Percent(-10)
    var turnOut = Percent(-10)
    out.foreach(o => {
      forwardOut = o.forward
      turnOut = o.turn
    })

    trigggerClock.apply(period)
    trigggerClock.apply(period)

    assert(forwardOut.toPercent == 50)
  }

  test("Forward position control returns correct proportional control (reverse)") {
    val drive = new TestDrivetrain

    val props = Signal.constant(new UnicycleProperties {
      override val maxForwardVelocity: Velocity = null
      override val maxTurnVelocity: AngularVelocity = null
      override val maxAcceleration: Acceleration = null
      override val defaultLookAheadDistance: Length = null
			override val trapezoidalProfileConfig: TrapezoidalProfileConfig[Velocity, Acceleration, Length] = null

      override val forwardControlGains = null
      override val turnControlGains = null

      override val forwardPositionControlGains = PIDConfig(
        Percent(100) / Meters(10),
        Percent(0) / (Meters(1).toGeneric * Seconds(1)),
        Percent(0) / MetersPerSecond(1)
      )

      override val turnPositionControlGains = null
    })

    val hardware: UnicycleHardware = new UnicycleHardware {
      override val forwardVelocity: Stream[Velocity] = Stream.periodic(period)(MetersPerSecond(0))
      override val turnVelocity: Stream[AngularVelocity] = Stream.periodic(period)(DegreesPerSecond(0))

      override val forwardPosition: Stream[Length] = Stream.periodic(period)(Meters(0))
      override val turnPosition: Stream[Angle] = null
    }

    val out = drive.UnicycleControllers.
      forwardPositionControl(Meters(-5))(hardware, props)._1

    var forwardOut = Percent(-10)
    var turnOut = Percent(-10)
    out.foreach(o => {
      forwardOut = o.forward
      turnOut = o.turn
    })

    trigggerClock.apply(period)
    trigggerClock.apply(period)

    assert(forwardOut.toPercent == -50)
  }

  test("Turn position control when relative angle is zero returns zero speed") {
    val drive = new TestDrivetrain

    val props = Signal.constant(new UnicycleProperties {
      override val maxForwardVelocity: Velocity = null
      override val maxTurnVelocity: AngularVelocity = null
      override val maxAcceleration: Acceleration = null
      override val defaultLookAheadDistance: Length = null
			override val trapezoidalProfileConfig: TrapezoidalProfileConfig[Velocity, Acceleration, Length] = null

      override val forwardControlGains = null
      override val turnControlGains = null

      override val forwardPositionControlGains = null

      override val turnPositionControlGains = PIDConfig(
        Percent(100) / Degrees(1),
        Percent(0) / (Degrees(1).toGeneric * Seconds(1)),
        Percent(0) / DegreesPerSecond(1)
      )
    })

    val hardware: UnicycleHardware = new UnicycleHardware {
      override val forwardVelocity: Stream[Velocity] = Stream.periodic(period)(MetersPerSecond(0))
      override val turnVelocity: Stream[AngularVelocity] = Stream.periodic(period)(DegreesPerSecond(0))

      override val forwardPosition: Stream[Length] = null
      override val turnPosition: Stream[Angle] = Stream.periodic(period)(Degrees(5))
    }

    val out = drive.UnicycleControllers.
      turnPositionControl(Degrees(5))(hardware, props)._1

    var forwardOut = Percent(-10)
    var turnOut = Percent(-10)
    out.foreach(o => {
      forwardOut = o.forward
      turnOut = o.turn
    })

    trigggerClock.apply(period)
    trigggerClock.apply(period)

    assert(turnOut.toPercent == 0)
  }

  test("Turn position control returns correct proportional control (clockwise)") {
    val drive = new TestDrivetrain

    val props = Signal.constant(new UnicycleProperties {
      override val maxForwardVelocity: Velocity = null
      override val maxTurnVelocity: AngularVelocity = null
      override val maxAcceleration: Acceleration = null
      override val defaultLookAheadDistance: Length = null
			override val trapezoidalProfileConfig: TrapezoidalProfileConfig[Velocity, Acceleration, Length] = null

      override val forwardControlGains = null
      override val turnControlGains = null

      override val forwardPositionControlGains = null

      override val turnPositionControlGains = PIDConfig(
        Percent(100) / Degrees(10),
        Percent(0) / (Degrees(1).toGeneric * Seconds(1)),
        Percent(0) / DegreesPerSecond(1)
      )
    })

    val hardware: UnicycleHardware = new UnicycleHardware {
      override val forwardVelocity: Stream[Velocity] = Stream.periodic(period)(MetersPerSecond(0))
      override val turnVelocity: Stream[AngularVelocity] = Stream.periodic(period)(DegreesPerSecond(0))

      override val forwardPosition: Stream[Length] = null
      override val turnPosition: Stream[Angle] = Stream.periodic(period)(Degrees(0))
    }

    val out = drive.UnicycleControllers.
      turnPositionControl(Degrees(5))(hardware, props)._1

    var forwardOut = Percent(-10)
    var turnOut = Percent(-10)
    out.foreach(o => {
      forwardOut = o.forward
      turnOut = o.turn
    })

    trigggerClock.apply(period)
    trigggerClock.apply(period)

    assert(turnOut.toPercent == 50)
  }

  test("Turn position control returns correct proportional control (counterclockwise)") {
    val drive = new TestDrivetrain

    val props = Signal.constant(new UnicycleProperties {
      override val maxForwardVelocity: Velocity = null
      override val maxTurnVelocity: AngularVelocity = null
      override val maxAcceleration: Acceleration = null
      override val defaultLookAheadDistance: Length = null
			override val trapezoidalProfileConfig: TrapezoidalProfileConfig[Velocity, Acceleration, Length] = null

      override val forwardControlGains = null
      override val turnControlGains = null

      override val forwardPositionControlGains = null

      override val turnPositionControlGains = PIDConfig(
        Percent(100) / Degrees(10),
        Percent(0) / (Degrees(1).toGeneric * Seconds(1)),
        Percent(0) / DegreesPerSecond(1)
      )
    })

    val hardware: UnicycleHardware = new UnicycleHardware {
      override val forwardVelocity: Stream[Velocity] = Stream.periodic(period)(MetersPerSecond(0))
      override val turnVelocity: Stream[AngularVelocity] = Stream.periodic(period)(DegreesPerSecond(0))

      override val forwardPosition: Stream[Length] = null
      override val turnPosition: Stream[Angle] = Stream.periodic(period)(Degrees(0))
    }

    val out = drive.UnicycleControllers.
      turnPositionControl(Degrees(-5))(hardware, props)._1

    var forwardOut = Percent(-10)
    var turnOut = Percent(-10)
    out.foreach(o => {
      forwardOut = o.forward
      turnOut = o.turn
    })

    trigggerClock.apply(period)
    trigggerClock.apply(period)

    assert(turnOut.toPercent == -50)
  }
}
