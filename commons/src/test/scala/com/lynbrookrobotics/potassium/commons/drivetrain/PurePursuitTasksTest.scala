package com.lynbrookrobotics.potassium.commons.drivetrain

import squants.space.Feet
import squants.{Acceleration, Angle, Each, Length, Percent, Velocity}
import com.lynbrookrobotics.potassium.control.PIDConfig
import com.lynbrookrobotics.potassium.testing.ClockMocking
import com.lynbrookrobotics.potassium.units.GenericValue._
import com.lynbrookrobotics.potassium.units._
import com.lynbrookrobotics.potassium.{Component, PeriodicSignal, Signal, SignalLike}
import squants.motion.{AngularVelocity, DegreesPerSecond, MetersPerSecond, MetersPerSecondSquared}
import squants.motion._
import squants.space.{Degrees, Meters}
import squants.time.{Milliseconds, Seconds}
import org.scalatest.FunSuite

class PurePursuitTasksTest extends FunSuite {
  class TestDrivetrain extends UnicycleDrive {
    override type DriveSignal = UnicycleSignal
    override type DriveVelocity = UnicycleSignal

    override type Hardware = UnicycleHardware
    override type Properties = UnicycleProperties

    override protected def convertUnicycleToDrive(uni: UnicycleSignal): DriveSignal = uni

    override protected def controlMode(implicit hardware: Hardware,
                                       props: Properties): UnicycleControlMode = NoOperation

    override protected def driveClosedLoop(signal: SignalLike[DriveSignal])
                                          (implicit hardware: Hardware,
                                           props: Signal[Properties]): PeriodicSignal[DriveSignal] = {
      UnicycleControllers.lowerLevelVelocityControl(signal)
    }

    override type Drivetrain = Component[DriveSignal]
  }

  val zeroSignal = UnicycleSignal(Each(0), Each(0))

  implicit val props = Signal.constant(new UnicycleProperties {
    override val maxForwardVelocity: Velocity = MetersPerSecond(10)
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
  })

  implicit val (clock, ticker) = ClockMocking.mockedClockTicker
  val drive = new TestDrivetrain

  class TestDrivetrainComponent(onTick: (UnicycleSignal) => Unit) extends Component[UnicycleSignal](Milliseconds(5)) {
    override def defaultController: PeriodicSignal[UnicycleSignal] =
      Signal.constant(UnicycleSignal(Percent(0), Percent(0))).toPeriodic

    override def applySignal(signal: UnicycleSignal): Unit = {
      onTick(signal)
    }
  }

  test("If target is initally within tolerance, stop immediately") {
    var lastAppliedSignal = zeroSignal
    implicit val drivetrainComp = new TestDrivetrainComponent(lastAppliedSignal = _)

    implicit val hardware = new UnicycleHardware {
      override val forwardVelocity: Signal[Velocity] = Signal(MetersPerSecond(0))
      override val turnVelocity: Signal[AngularVelocity] = Signal(DegreesPerSecond(0))

      override val forwardPosition: Signal[Length] = Signal.constant(Feet(0))
      override val turnPosition: Signal[Angle] = Signal.constant(Degrees(0))
    }

    val target = new Point(Feet(0), Feet(0.5))
    val task = new drive.unicycleTasks.FollowWayPoints(
      Seq(Point.origin, target), Feet(1))

    task.init()

    ticker(Milliseconds(5))
    assert(!task.isRunning)
  }

  test("Test that having 1 way point directly ahead results in not turning") {
    var lastAppliedSignal = zeroSignal
    implicit val testDrivetrainComp = new TestDrivetrainComponent(lastAppliedSignal = _)

    val target = new Point(Feet(0), Feet(1))

    implicit val hardware = new UnicycleHardware {
      override val forwardVelocity: Signal[Velocity] = null

      override val turnVelocity: Signal[AngularVelocity] = Signal(DegreesPerSecond(0))

      override val forwardPosition: Signal[Length] = Signal.constant(Feet(0))
      override val turnPosition: Signal[Angle] = Signal.constant(Degrees(0))
    }

    val task = new drive.unicycleTasks.FollowWayPoints(Seq(Point.origin, target), Feet(0.1))

    task.init()

    ticker(Milliseconds(5))

    assert(lastAppliedSignal.turn.toPercent == 0)

    implicit val tolerance = Each(0.01)
    assert(lastAppliedSignal.forward ~= Percent(100))
  }

  test("Test that going left and back 1 foot does not result in full turn"){
    var lastAppliedSignal = zeroSignal
    implicit val drivetrainComp = new TestDrivetrainComponent(lastAppliedSignal = _)

    var curTurnPosition = Degrees(0)

    implicit val hardware = new UnicycleHardware {
      override val forwardVelocity: Signal[Velocity] = Signal(MetersPerSecond(0))
      override val turnVelocity: Signal[AngularVelocity] = Signal(DegreesPerSecond(0))
      var askedForInitPosition = false

      override val forwardPosition: Signal[Length] = Signal.constant(Feet(0))
      override val turnPosition: Signal[Angle] = Signal(curTurnPosition)
    }

    val target = new Point(Feet(-1), Feet(-1))
    val task = new drive.unicycleTasks.FollowWayPoints(Seq(Point.origin, target), Feet(1))

    task.init()

    curTurnPosition = Degrees(45)
    ticker(Milliseconds(5))

    implicit val tolerance = Percent(0.01)
    val result = lastAppliedSignal.turn

    assert(result ~= Percent(0))
  }
}

