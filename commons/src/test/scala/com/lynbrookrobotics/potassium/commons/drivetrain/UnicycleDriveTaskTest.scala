package com.lynbrookrobotics.potassium.commons.drivetrain

import com.lynbrookrobotics.potassium.control.PIDFConfig
import com.lynbrookrobotics.potassium.testing.ClockMocking
import com.lynbrookrobotics.potassium.units.GenericValue._
import com.lynbrookrobotics.potassium.units._
import com.lynbrookrobotics.potassium.{Component, PeriodicSignal, Signal, SignalLike}

import squants.motion.{AngularVelocity, DegreesPerSecond, MetersPerSecond, MetersPerSecondSquared}
import squants.space.{Degrees, Meters}
import squants.time.{Milliseconds, Seconds}
import squants.{Angle, Length, Percent, Velocity}

import org.scalatest.FunSuite

class UnicycleDriveTaskTest extends FunSuite {
  implicit val hardware: UnicycleHardware = new UnicycleHardware {
    override val forwardVelocity: Signal[Velocity] = Signal(MetersPerSecond(0))
    override val turnVelocity: Signal[AngularVelocity] = Signal(DegreesPerSecond(0))

    override def forwardPosition: Signal[Length] = ???
    override def turnPosition: Signal[Angle] = ???
  }

  private class TestDrivetrain extends UnicycleDrive {
    override type DriveSignal = UnicycleSignal
    override type DriveVelocity = UnicycleSignal

    override type DrivetrainHardware = UnicycleHardware
    override type DrivetrainProperties = UnicycleProperties

    override protected def convertUnicycleToDrive(uni: UnicycleSignal): DriveSignal = uni

    override protected def controlMode(implicit hardware: DrivetrainHardware,
                                       props: DrivetrainProperties): UnicycleControlMode = NoOperation

    override protected def driveClosedLoop(signal: SignalLike[DriveSignal])
                                          (implicit hardware: DrivetrainHardware,
                                           props: DrivetrainProperties): PeriodicSignal[DriveSignal] = signal.toPeriodic

    override type Drivetrain = Component[DriveSignal]
  }

  test("Drive distance task sets up correct relative position and ends at target") {
    val drive = new TestDrivetrain

    val props = new UnicycleProperties {
      override val maxForwardVelocity: Velocity = MetersPerSecond(10)
      override def maxTurnVelocity: AngularVelocity = DegreesPerSecond(10)

      override val forwardControlGains = PIDFConfig(
        Percent(0) / MetersPerSecond(1),
        Percent(0) / Meters(1),
        Percent(0) / MetersPerSecondSquared(1),
        Percent(100) / maxForwardVelocity
      )

      override val turnControlGains = PIDFConfig(
        Percent(0) / DegreesPerSecond(1),
        Percent(0) / (DegreesPerSecond(1).toGeneric * Seconds(1)),
        Percent(0) / (DegreesPerSecond(1).toGeneric / Seconds(1)),
        Percent(100) / maxTurnVelocity
      )

      override val forwardPositionControlGains = PIDFConfig(
        Percent(100) / Meters(10),
        Percent(0) / (Meters(1).toGeneric * Seconds(1)),
        Percent(0) / MetersPerSecond(1),
        Percent(0) / Meters(1)
      )

      override def turnPositionControlGains = ???
    }

    var lastAppliedSignal: UnicycleSignal = null

    implicit val (clock, ticker) = ClockMocking.mockedClockTicker

    val drivetrain = new Component[UnicycleSignal](Milliseconds(5)) {
      override def defaultController: PeriodicSignal[UnicycleSignal] =
        Signal.constant(UnicycleSignal(Percent(0), Percent(0))).toPeriodic

      override def applySignal(signal: UnicycleSignal): Unit = {
        lastAppliedSignal = signal
      }
    }

    var currentPosition = Meters(5)

    val hardware: UnicycleHardware = new UnicycleHardware {
      override val forwardVelocity: Signal[Velocity] = Signal(MetersPerSecond(0))
      override val turnVelocity: Signal[AngularVelocity] = Signal(DegreesPerSecond(0))

      override def forwardPosition: Signal[Length] = Signal(currentPosition)
      override def turnPosition: Signal[Angle] = ???
    }

    val task = new drive.unicycleTasks.DriveDistance(
      Meters(5), Meters(0.1))(
      drivetrain,
      hardware,
      props
    )

    task.init()

    ticker(Milliseconds(5))

    assert(lastAppliedSignal.forward.toPercent == 50)

    currentPosition = Meters(7.5)

    ticker(Milliseconds(5))

    assert(lastAppliedSignal.forward.toPercent == 25)

    currentPosition = Meters(10)

    ticker(Milliseconds(5))

    assert(lastAppliedSignal.forward.toPercent == 0 && !task.isRunning)
  }

  test("Drive distance straight task sets up correct relative position and ends at target") {
    val drive = new TestDrivetrain

    val props = new UnicycleProperties {
      override val maxForwardVelocity: Velocity = MetersPerSecond(10)
      override def maxTurnVelocity: AngularVelocity = DegreesPerSecond(10)

      override val forwardControlGains = PIDFConfig(
        Percent(0) / MetersPerSecond(1),
        Percent(0) / Meters(1),
        Percent(0) / MetersPerSecondSquared(1),
        Percent(100) / maxForwardVelocity
      )

      override val turnControlGains = PIDFConfig(
        Percent(0) / DegreesPerSecond(1),
        Percent(0) / (DegreesPerSecond(1).toGeneric * Seconds(1)),
        Percent(0) / (DegreesPerSecond(1).toGeneric / Seconds(1)),
        Percent(100) / maxTurnVelocity
      )

      override val forwardPositionControlGains = PIDFConfig(
        Percent(100) / Meters(10),
        Percent(0) / (Meters(1).toGeneric * Seconds(1)),
        Percent(0) / MetersPerSecond(1),
        Percent(0) / Meters(1)
      )

      override val turnPositionControlGains = PIDFConfig(
        Percent(100) / Degrees(10),
        Percent(0) / (Degrees(1).toGeneric * Seconds(1)),
        Percent(0) / DegreesPerSecond(1),
        Percent(0) / Degrees(1)
      )
    }

    var lastAppliedSignal: UnicycleSignal = null

    implicit val (clock, ticker) = ClockMocking.mockedClockTicker

    val drivetrain = new Component[UnicycleSignal](Milliseconds(5)) {
      override def defaultController: PeriodicSignal[UnicycleSignal] =
        Signal.constant(UnicycleSignal(Percent(0), Percent(0))).toPeriodic

      override def applySignal(signal: UnicycleSignal): Unit = {
        lastAppliedSignal = signal
      }
    }

    var currentPositionForward = Meters(5)
    var currentPositionTurn = Degrees(5)

    val hardware: UnicycleHardware = new UnicycleHardware {
      override val forwardVelocity: Signal[Velocity] = Signal(MetersPerSecond(0))
      override val turnVelocity: Signal[AngularVelocity] = Signal(DegreesPerSecond(0))

      override def forwardPosition: Signal[Length] = Signal(currentPositionForward)
      override def turnPosition: Signal[Angle] = Signal(currentPositionTurn)
    }

    val task = new drive.unicycleTasks.DriveDistanceStraight(
      Meters(5), Meters(0.1), Degrees(0.1))(
      drivetrain,
      hardware,
      props
    )

    task.init()

    ticker(Milliseconds(5))

    assert(lastAppliedSignal.forward.toPercent == 50 &&
      lastAppliedSignal.turn.toPercent == 0)

    currentPositionForward = Meters(7.5)
    currentPositionTurn = Degrees(10)

    ticker(Milliseconds(5))

    assert(lastAppliedSignal.forward.toPercent == 25 &&
      lastAppliedSignal.turn.toPercent == -50)

    currentPositionForward = Meters(10)
    currentPositionTurn = Degrees(0)

    ticker(Milliseconds(5))

    assert(lastAppliedSignal.forward.toPercent == 0 &&
      lastAppliedSignal.turn.toPercent == 50)

    currentPositionForward = Meters(10)
    currentPositionTurn = Degrees(5)

    ticker(Milliseconds(5))

    assert(lastAppliedSignal.forward.toPercent == 0 &&
      lastAppliedSignal.turn.toPercent == 0 &&
      !task.isRunning)
  }

  test("Turn angle task sets up correct relative position and ends at target") {
    val drive = new TestDrivetrain

    val props = new UnicycleProperties {
      override val maxForwardVelocity: Velocity = MetersPerSecond(10)
      override def maxTurnVelocity: AngularVelocity = DegreesPerSecond(10)

      override val forwardControlGains = PIDFConfig(
        Percent(0) / MetersPerSecond(1),
        Percent(0) / Meters(1),
        Percent(0) / MetersPerSecondSquared(1),
        Percent(100) / maxForwardVelocity
      )

      override val turnControlGains = PIDFConfig(
        Percent(0) / DegreesPerSecond(1),
        Percent(0) / (DegreesPerSecond(1).toGeneric * Seconds(1)),
        Percent(0) / (DegreesPerSecond(1).toGeneric / Seconds(1)),
        Percent(100) / maxTurnVelocity
      )

      override def forwardPositionControlGains = ???

      override def turnPositionControlGains = PIDFConfig(
        Percent(100) / Degrees(10),
        Percent(0) / (Degrees(1).toGeneric * Seconds(1)),
        Percent(0) / DegreesPerSecond(1),
        Percent(0) / Degrees(1)
      )
    }

    var lastAppliedSignal: UnicycleSignal = null

    implicit val (clock, ticker) = ClockMocking.mockedClockTicker

    val drivetrain = new Component[UnicycleSignal](Milliseconds(5)) {
      override def defaultController: PeriodicSignal[UnicycleSignal] =
        Signal.constant(UnicycleSignal(Percent(0), Percent(0))).toPeriodic

      override def applySignal(signal: UnicycleSignal): Unit = {
        lastAppliedSignal = signal
      }
    }

    var currentPosition = Degrees(5)

    val hardware: UnicycleHardware = new UnicycleHardware {
      override val forwardVelocity: Signal[Velocity] = Signal(MetersPerSecond(0))
      override val turnVelocity: Signal[AngularVelocity] = Signal(DegreesPerSecond(0))

      override def forwardPosition: Signal[Length] = ???
      override def turnPosition: Signal[Angle] = Signal(currentPosition)
    }

    val task = new drive.unicycleTasks.RotateByAngle(
      Degrees(5), Degrees(0.1))(
      drivetrain,
      hardware,
      props
    )

    task.init()

    ticker(Milliseconds(5))

    assert(lastAppliedSignal.turn.toPercent == 50)

    currentPosition = Degrees(7.5)

    ticker(Milliseconds(5))

    assert(lastAppliedSignal.turn.toPercent == 25)

    currentPosition = Degrees(10)

    ticker(Milliseconds(5))

    assert(lastAppliedSignal.turn.toPercent == 0 && !task.isRunning)
  }
}
