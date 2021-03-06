package com.lynbrookrobotics.potassium.commons.drivetrain

import com.lynbrookrobotics.potassium.control.PIDConfig
import com.lynbrookrobotics.potassium.ClockMocking
import com.lynbrookrobotics.potassium.streams.{Periodic, Stream}
import com.lynbrookrobotics.potassium.units.GenericValue._
import com.lynbrookrobotics.potassium.units._
import com.lynbrookrobotics.potassium._
import com.lynbrookrobotics.potassium.commons.drivetrain.unicycle.{UnicycleDrive, UnicycleHardware, UnicycleProperties, UnicycleSignal}
import squants.motion.{AngularVelocity, DegreesPerSecond, MetersPerSecond, MetersPerSecondSquared}
import squants.motion._
import squants.space.{Degrees, Feet, Meters}
import squants.time.{Milliseconds, Seconds}
import squants.{Acceleration, Angle, Length, Percent, Time, Velocity}
import org.scalatest.FunSuite

class UnicycleDriveTaskTest extends FunSuite {
  class TestDrivetrain extends UnicycleDrive {
    override type DriveSignal = UnicycleSignal
    override type OpenLoopSignal = UnicycleSignal

    override type Hardware = UnicycleHardware
    override type Properties = UnicycleProperties

    override protected def unicycleToOpenLoopSignal(uni: UnicycleSignal): DriveSignal = uni

    override protected def controlMode(implicit hardware: Hardware, props: Properties): UnicycleControlMode =
      NoOperation

    override protected def driveClosedLoop(
      signal: Stream[UnicycleSignal]
    )(implicit hardware: UnicycleHardware, props: Signal[UnicycleProperties]): Stream[UnicycleSignal] = signal

    override protected def openLoopToDriveSignal(openLoop: UnicycleSignal): UnicycleSignal = openLoop

    override type Drivetrain = Component[DriveSignal]
  }

  val tickPeriod = Milliseconds(5)
  implicit val (clock, ticker) = ClockMocking.mockedClockTicker

  implicit val hardware: UnicycleHardware = new UnicycleHardware {
    // MetersPerSecond(0)
    override val forwardVelocity: Stream[Velocity] = Stream.periodic(tickPeriod)(MetersPerSecond(0))
    override val turnVelocity: Stream[AngularVelocity] = Stream.periodic(tickPeriod)(DegreesPerSecond(0))

    override val forwardPosition: Stream[Length] = null
    override val turnPosition: Stream[Angle] = null
  }

  test("Drive distance task sets up correct relative position and ends at target") {
    val drive = new TestDrivetrain

    val props = Signal.constant(new UnicycleProperties {
      override val maxForwardVelocity: Velocity = MetersPerSecond(10)
      override val maxTurnVelocity: AngularVelocity = DegreesPerSecond(10)
      override val maxAcceleration: Acceleration = FeetPerSecondSquared(15)
      override val maxDeceleration: Acceleration = FeetPerSecondSquared(15)
      override val defaultLookAheadDistance: Length = null

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
        Percent(100) / Meters(10),
        Percent(0) / (Meters(1).toGeneric * Seconds(1)),
        Percent(0) / MetersPerSecond(1)
      )

      override val turnPositionGains: TurnPositionGains = null
    })

    var lastAppliedSignal: UnicycleSignal = null

    val drivetrain = new Component[UnicycleSignal] {
      override def defaultController: Stream[UnicycleSignal] =
        Stream.periodic(tickPeriod)(UnicycleSignal(Percent(0), Percent(0)))

      override def applySignal(signal: UnicycleSignal): Unit = {
        lastAppliedSignal = signal
      }
    }

    var currentPosition = Meters(5)

    val hardware: UnicycleHardware = new UnicycleHardware {
      val rootStream = Stream.periodic(tickPeriod)(())
      override val forwardVelocity: Stream[Velocity] = rootStream.mapToConstant(MetersPerSecond(0))
      override val turnVelocity: Stream[AngularVelocity] = rootStream.mapToConstant(DegreesPerSecond(0))

      override val forwardPosition: Stream[Length] = rootStream.map(_ => currentPosition)
      override val turnPosition: Stream[Angle] = null
    }

    val task = new drive.unicycleTasks.DriveDistance(
      Meters(5),
      Meters(0.1)
    )(drivetrain)(hardware, props)

    task.init()

    ticker(Milliseconds(5))
    ticker(Milliseconds(5))
    ticker(Milliseconds(5))

    assert(lastAppliedSignal.forward.toPercent == 50)

    currentPosition = Meters(7.5)

    ticker(Milliseconds(5))

    assert(lastAppliedSignal.forward.toPercent == 25)

    currentPosition = Meters(10)

    ticker(Milliseconds(5))

    assert(!task.isRunning)
  }

  test("Drive distance straight task sets up correct relative position and ends at target") {
    val drive = new TestDrivetrain

    val props = Signal.constant(new UnicycleProperties {
      override val maxForwardVelocity: Velocity = MetersPerSecond(10)
      override val maxTurnVelocity: AngularVelocity = DegreesPerSecond(10)
      override val maxAcceleration: Acceleration = FeetPerSecondSquared(10)
      override val maxDeceleration: Acceleration = FeetPerSecondSquared(10)
      override val defaultLookAheadDistance: Length = null

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
        Percent(100) / Meters(10),
        Percent(0) / (Meters(1).toGeneric * Seconds(1)),
        Percent(0) / MetersPerSecond(1)
      )

      override val turnPositionGains: TurnPositionGains = PIDConfig(
        Percent(100) / Degrees(10),
        Percent(0) / (Degrees(1).toGeneric * Seconds(1)),
        Percent(0) / DegreesPerSecond(1)
      )
    })

    var lastAppliedSignal: UnicycleSignal = null

    val drivetrain = new Component[UnicycleSignal] {
      override def defaultController: Stream[UnicycleSignal] =
        Stream.periodic(tickPeriod)(UnicycleSignal(Percent(0), Percent(0)))

      override def applySignal(signal: UnicycleSignal): Unit = {
        lastAppliedSignal = signal
      }
    }

    var currentPositionForward = Meters(5)
    var currentPositionTurn = Degrees(5)

    val hardware: UnicycleHardware = new UnicycleHardware {
      val rootStream = Stream.periodic(tickPeriod)(())
      override val forwardVelocity: Stream[Velocity] = rootStream.mapToConstant(MetersPerSecond(0))
      override val turnVelocity: Stream[AngularVelocity] = rootStream.mapToConstant(DegreesPerSecond(0))

      override val forwardPosition: Stream[Length] = rootStream.map(_ => currentPositionForward)
      override val turnPosition: Stream[Angle] = rootStream.map(_ => currentPositionTurn)
    }

    val task = new drive.unicycleTasks.DriveDistanceStraight(
      Meters(5),
      Meters(0.1),
      Degrees(0.1),
      Percent(100),
      minStableTicks = 1
    )(drivetrain)(
      hardware,
      props
    )

    task.init()

    ticker(Milliseconds(5))
    ticker(Milliseconds(5))
    ticker(Milliseconds(5))

    assert(
      lastAppliedSignal.forward.toPercent == 50 &&
      lastAppliedSignal.turn.toPercent == 0
    )

    currentPositionForward = Meters(7.5)
    currentPositionTurn = Degrees(10)

    ticker(Milliseconds(5))

    assert(
      lastAppliedSignal.forward.toPercent == 25 &&
      lastAppliedSignal.turn.toPercent == -50
    )

    currentPositionForward = Meters(10)
    currentPositionTurn = Degrees(0)

    ticker(Milliseconds(5))

    assert(
      lastAppliedSignal.forward.toPercent == 0 &&
      lastAppliedSignal.turn.toPercent == 50
    )

    currentPositionForward = Meters(10)
    currentPositionTurn = Degrees(5)

    ticker(Milliseconds(5))

    assert(!task.isRunning)
  }

  test("Drive distance at angle task sets up correct relative position and ends at target") {
    val drive = new TestDrivetrain

    val props = Signal.constant(new UnicycleProperties {
      override val maxForwardVelocity: Velocity = MetersPerSecond(10)
      override val maxTurnVelocity: AngularVelocity = DegreesPerSecond(10)
      override val maxAcceleration: Acceleration = FeetPerSecondSquared(10)
      override val maxDeceleration: Acceleration = FeetPerSecondSquared(10)
      override val defaultLookAheadDistance: Length = null

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
        Percent(100) / Meters(10),
        Percent(0) / (Meters(1).toGeneric * Seconds(1)),
        Percent(0) / MetersPerSecond(1)
      )

      override val turnPositionGains: TurnPositionGains = PIDConfig(
        Percent(100) / Degrees(10),
        Percent(0) / (Degrees(1).toGeneric * Seconds(1)),
        Percent(0) / DegreesPerSecond(1)
      )
    })

    var lastAppliedSignal: UnicycleSignal = null

    val drivetrain = new Component[UnicycleSignal] {
      override def defaultController: Stream[UnicycleSignal] =
        Stream.periodic(tickPeriod)(UnicycleSignal(Percent(0), Percent(0)))

      override def applySignal(signal: UnicycleSignal): Unit = {
        lastAppliedSignal = signal
      }
    }

    var currentPositionForward = Meters(5)
    var currentPositionTurn = Degrees(45)

    val hardware: UnicycleHardware = new UnicycleHardware {
      val rootStream = Stream.periodic(tickPeriod)(())
      override val forwardVelocity: Stream[Velocity] = rootStream.mapToConstant(MetersPerSecond(0))
      override val turnVelocity: Stream[AngularVelocity] = rootStream.mapToConstant(DegreesPerSecond(0))

      override val forwardPosition: Stream[Length] = rootStream.map(_ => currentPositionForward)
      override val turnPosition: Stream[Angle] = rootStream.map(_ => currentPositionTurn)
    }

    val task = new drive.unicycleTasks.DriveDistanceAtAngle(
      Meters(5),
      Meters(0.1),
      Degrees(45),
      Degrees(0.1),
      Percent(100)
    )(drivetrain)(
      hardware,
      props
    )

    task.init()

    ticker(Milliseconds(5))
    ticker(Milliseconds(5))
    ticker(Milliseconds(5))

    assert(
      lastAppliedSignal.forward.toPercent == 50 &&
      lastAppliedSignal.turn.toPercent == 0
    )

    currentPositionForward = Meters(7.5)
    currentPositionTurn = Degrees(45 - 5)

    ticker(Milliseconds(5))

    assert(
      lastAppliedSignal.forward.toPercent == 25 &&
      lastAppliedSignal.turn.toPercent == 50
    )

    currentPositionForward = Meters(10)
    currentPositionTurn = Degrees(45 + 5)

    ticker(Milliseconds(5))

    assert(
      lastAppliedSignal.forward.toPercent == 0 &&
      lastAppliedSignal.turn.toPercent == -50
    )

    currentPositionForward = Meters(10)
    currentPositionTurn = Degrees(45)

    ticker(Milliseconds(5))

    assert(!task.isRunning)
  }

  test("Turn angle task sets up correct relative position and ends at target") {
    val drive = new TestDrivetrain

    val props = Signal.constant(new UnicycleProperties {
      override val maxForwardVelocity: Velocity = MetersPerSecond(10)
      override val maxTurnVelocity: AngularVelocity = DegreesPerSecond(10)
      override val maxAcceleration: Acceleration = FeetPerSecondSquared(10)
      override val maxDeceleration: Acceleration = FeetPerSecondSquared(10)
      override val defaultLookAheadDistance: Length = null

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

      override val forwardPositionGains: ForwardPositionGains = null

      override val turnPositionGains: TurnPositionGains = PIDConfig(
        Percent(100) / Degrees(10),
        Percent(0) / (Degrees(1).toGeneric * Seconds(1)),
        Percent(0) / DegreesPerSecond(1)
      )
    })

    var lastAppliedSignal: UnicycleSignal = null

    val drivetrain = new Component[UnicycleSignal] {
      override def defaultController: Stream[UnicycleSignal] =
        Stream.periodic(tickPeriod)(UnicycleSignal(Percent(0), Percent(0)))

      override def applySignal(signal: UnicycleSignal): Unit = {
        lastAppliedSignal = signal
      }
    }

    var currentPosition = Degrees(5)

    val hardware: UnicycleHardware = new UnicycleHardware {
      val rootStream = Stream.periodic(tickPeriod)(())
      override val forwardVelocity: Stream[Velocity] = rootStream.mapToConstant(MetersPerSecond(0))
      override val turnVelocity: Stream[AngularVelocity] = rootStream.mapToConstant(DegreesPerSecond(0))

      override val forwardPosition: Stream[Length] = null
      override val turnPosition: Stream[Angle] = rootStream.map(_ => currentPosition)
    }

    val task = new drive.unicycleTasks.RotateByAngle(
      Degrees(5),
      Degrees(0.1),
      1
    )(drivetrain)(
      hardware,
      props
    )

    task.init()

    ticker(Milliseconds(5))
    ticker(Milliseconds(5))
    ticker(Milliseconds(5))

    assert(lastAppliedSignal.turn.toPercent == 50)

    currentPosition = Degrees(7.5)

    ticker(Milliseconds(5))

    assert(lastAppliedSignal.turn.toPercent == 25)

    currentPosition = Degrees(10)

    ticker(Milliseconds(5))

    assert(!task.isRunning)
  }

  test("Task finishes when robot is within tolerance distance") {
    val drive = new TestDrivetrain

    val props = Signal.constant(new UnicycleProperties {
      override val maxForwardVelocity: Velocity = MetersPerSecond(10)
      override val maxTurnVelocity: AngularVelocity = DegreesPerSecond(10)
      override val maxAcceleration: Acceleration = FeetPerSecondSquared(10)
      override val maxDeceleration: Acceleration = FeetPerSecondSquared(10)
      override val defaultLookAheadDistance: Length = null

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

      override val forwardPositionGains: ForwardPositionGains = null

      override val turnPositionGains: TurnPositionGains = PIDConfig(
        Percent(100) / Degrees(50),
        Percent(0) / (Degrees(1).toGeneric * Seconds(1)),
        Percent(0) / DegreesPerSecond(1)
      )
    })

    var lastAppliedSignal: UnicycleSignal = null

    val drivetrain = new Component[UnicycleSignal] {
      override def defaultController: Stream[UnicycleSignal] =
        Stream.periodic(tickPeriod)(UnicycleSignal(Percent(0), Percent(0)))

      override def applySignal(signal: UnicycleSignal): Unit = {
        lastAppliedSignal = signal
      }
    }

    var currentPosition = Meters(0)
    var currentAngle = Degrees(0)

    val hardware: UnicycleHardware = new UnicycleHardware {
      val rootStream = Stream.periodic(tickPeriod)(())
      override val turnPosition: Stream[Angle] = rootStream.map(_ => currentAngle)
      override val turnVelocity: Stream[AngularVelocity] = turnPosition.derivative

      override val forwardPosition: Stream[Length] = rootStream.map(_ => currentPosition)
      override val forwardVelocity: Stream[Velocity] = forwardPosition.derivative
    }

    val distanceToTarget = hardware.forwardPosition.map(p => Some(Meters(2) - p))
    val angleToTarget = hardware.turnPosition.map(p => Degrees(90) - p)

    val task = new drive.unicycleTasks.DriveToTargetWithConstantSpeed(
      drivetrain,
      distanceToTarget,
      angleToTarget,
      Percent(15),
      Percent(20),
      Meters(1)
    )(hardware, props)

    task.init()

    ticker(Milliseconds(5))
    ticker(Milliseconds(5))
    ticker(Milliseconds(5))

    assert(lastAppliedSignal.forward == Percent(15))
    assert(lastAppliedSignal.turn == Percent(20))

    currentPosition = Meters(0.5)
    currentAngle = Degrees(90)

    ticker(Milliseconds(5))

    assert(lastAppliedSignal.forward == Percent(15))
    assert(lastAppliedSignal.turn == Percent(0))

    currentPosition = Meters(1.0)

    ticker(Milliseconds(5))

    assert(!task.isRunning)
  }
}
