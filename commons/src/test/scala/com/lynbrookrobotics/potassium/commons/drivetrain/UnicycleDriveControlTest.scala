package com.lynbrookrobotics.potassium.commons.drivetrain

import com.lynbrookrobotics.potassium.commons.drivetrain.unicycle._
import com.lynbrookrobotics.potassium.control.PIDConfig
import com.lynbrookrobotics.potassium.streams.Stream
import com.lynbrookrobotics.potassium.units.GenericValue._
import com.lynbrookrobotics.potassium.units._
import com.lynbrookrobotics.potassium.{ClockMocking, Signal}
import org.scalacheck.Prop.forAll
import org.scalacheck.{Arbitrary, Gen}
import org.scalatest.FunSuite
import org.scalatest.prop.Checkers._
import squants.motion._
import squants.space.{Degrees, Meters}
import squants.time.{Milliseconds, Seconds}
import squants.{Acceleration, Angle, Each, Length, Percent, Velocity}

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
    override type OpenLoopInput = UnicycleSignal

    override type Hardware = UnicycleHardware
    override type Properties = UnicycleProperties

    override protected def convertUnicycleToOpenLoopInput(uni: UnicycleSignal): DriveSignal = uni

    override protected def getControlMode(implicit hardware: Hardware,
                                          props: Properties): UnicycleControlMode = NoOperation

    override type Drivetrain = Nothing

    override protected def driveClosedLoop(signal: Stream[OpenLoopInput])
                                          (implicit hardware: UnicycleHardware,
                                           props: Signal[UnicycleProperties]): Stream[UnicycleSignal] = signal

    override protected def openLoopToDriveSignal(openLoopInput: OpenLoopInput): UnicycleSignal = openLoopInput
  }

  implicit val arbitraryVelocity: Arbitrary[Velocity] = Arbitrary(
    Gen.chooseNum[Double](-100D, 100D).map(d => MetersPerSecond(d))
  )

  implicit val arbitraryAngularVelocity: Arbitrary[AngularVelocity] = Arbitrary(
    Gen.chooseNum[Double](-100D, 100D).map(d => DegreesPerSecond(d))
  )

  test("Closed loop with only feed-forward is essentially open loop") {
    implicit val props = Signal.constant(new UnicycleProperties {
      override val maxForwardVelocity: Velocity = MetersPerSecond(10)
      override val maxTurnVelocity: AngularVelocity = DegreesPerSecond(10)
      override val maxAcceleration: Acceleration = FeetPerSecondSquared(10)
      override val defaultLookAheadDistance: Length = null

      override val forwardVelocityGains = PIDConfig(
        Percent(0) / MetersPerSecond(1),
        Percent(0) / Meters(1),
        Percent(0) / MetersPerSecondSquared(1)
      )

      override val turnControlGains = PIDConfig(
        Percent(0) / DegreesPerSecond(1),
        Percent(0) / Degrees(1),
        Percent(0) / (DegreesPerSecond(1).toGeneric / Seconds(1))
      )

      override val forwardPositionGains = null
      override val turnPositionGains = null
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

      override val forwardVelocityGains = null
      override val turnControlGains = null

      override val forwardPositionGains = PIDConfig(
        Percent(100) / Meters(1),
        Percent(0) / (Meters(1).toGeneric * Seconds(1)),
        Percent(0) / MetersPerSecond(1)
      )

      override val turnPositionGains = null
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

      override val forwardVelocityGains = null
      override val turnControlGains = null

      override val forwardPositionGains = PIDConfig(
        Percent(100) / Meters(10),
        Percent(0) / (Meters(1).toGeneric * Seconds(1)),
        Percent(0) / MetersPerSecond(1)
      )

      override val turnPositionGains = null
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

      override val forwardVelocityGains = null
      override val turnControlGains = null

      override val forwardPositionGains = PIDConfig(
        Percent(100) / Meters(10),
        Percent(0) / (Meters(1).toGeneric * Seconds(1)),
        Percent(0) / MetersPerSecond(1)
      )

      override val turnPositionGains = null
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

      override val forwardVelocityGains = null
      override val turnControlGains = null

      override val forwardPositionGains = null

      override val turnPositionGains = PIDConfig(
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

      override val forwardVelocityGains = null
      override val turnControlGains = null

      override val forwardPositionGains = null

      override val turnPositionGains = PIDConfig(
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

      override val forwardVelocityGains = null
      override val turnControlGains = null

      override val forwardPositionGains = null

      override val turnPositionGains = PIDConfig(
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
