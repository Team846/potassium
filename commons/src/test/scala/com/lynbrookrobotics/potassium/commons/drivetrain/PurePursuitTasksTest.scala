package com.lynbrookrobotics.potassium.commons.drivetrain

import squants.space.Feet
import squants.{Acceleration, Angle, Each, Length, Percent, Velocity}
import com.lynbrookrobotics.potassium.streams.Stream
import com.lynbrookrobotics.potassium.control.PIDConfig
import com.lynbrookrobotics.potassium.units.GenericValue._
import com.lynbrookrobotics.potassium.units._
import com.lynbrookrobotics.potassium._
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

    override protected def driveClosedLoop(signal: Stream[DriveSignal])
                                          (implicit hardware: Hardware,
                                           props: Signal[Properties]): Stream[DriveSignal] = {
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

  val period = Milliseconds(5)
  implicit val (clock, ticker) = ClockMocking.mockedClockTicker
  val drive = new TestDrivetrain

  class TestDrivetrainComponent(onTick: UnicycleSignal => Unit) extends Component[UnicycleSignal](period) {
    override def defaultController: Stream[UnicycleSignal] =
      Stream.periodic(period)(UnicycleSignal(Percent(0), Percent(0)))

    override def applySignal(signal: UnicycleSignal): Unit = {
      onTick(signal)
    }
  }

  test("If target is initally within tolerance, stop immediately") {
    var lastAppliedSignal = zeroSignal
    val drivetrainComp = new TestDrivetrainComponent(lastAppliedSignal = _)

    implicit val hardware = new UnicycleHardware {
      override val forwardVelocity: Stream[Velocity] = Stream.periodic(period)(MetersPerSecond(0))
      override val turnVelocity: Stream[AngularVelocity] = Stream.periodic(period)(DegreesPerSecond(0))

      override val forwardPosition: Stream[Length] = Stream.periodic(period)(Feet(0))
      override val turnPosition: Stream[Angle] = Stream.periodic(period)(Degrees(0))
    }

    val target = new Point(Feet(0), Feet(0.5))
    val task = new drive.unicycleTasks.FollowWayPoints(Seq(Point.origin, target), Feet(1), Percent(70))(drivetrainComp)

    task.init()

    ticker(Milliseconds(5))
    ticker(Milliseconds(5))
    ticker(Milliseconds(5))

    assert(!task.isRunning)
  }

  test("Test that having 1 way point directly ahead results in not turning") {
    var lastAppliedSignal = zeroSignal
    val testDrivetrainComp = new TestDrivetrainComponent(lastAppliedSignal = _)

    val target = new Point(Feet(0), Feet(1))

    implicit val hardware = new UnicycleHardware {
      override val forwardVelocity: Stream[Velocity] = null

      override val turnVelocity: Stream[AngularVelocity] = Stream.periodic(period)(DegreesPerSecond(0))

      override val forwardPosition: Stream[Length] = Stream.periodic(period)(Feet(0))
      override val turnPosition: Stream[Angle] = Stream.periodic(period)(Degrees(0))
    }

    val task = new drive.unicycleTasks.FollowWayPoints(Seq(Point.origin, target), Feet(0.1), Percent(70))(testDrivetrainComp)

    task.init()

    ticker(Milliseconds(5))
    ticker(Milliseconds(5))
    ticker(Milliseconds(5))

    assert(lastAppliedSignal.turn.toPercent == 0, s"Turn was ${lastAppliedSignal.turn.toPercent} %")

    implicit val tolerance = Each(0.01)
    assert(lastAppliedSignal.forward ~= Percent(70), s"actual forward ${lastAppliedSignal.forward.toPercent}%")
  }

  test("Test that going left and back 1 foot does not result in full turn"){
    var lastAppliedSignal = zeroSignal
    val drivetrainComp = new TestDrivetrainComponent(lastAppliedSignal = _)

    implicit val hardware = new UnicycleHardware {
      override val forwardVelocity: Stream[Velocity] = Stream.periodic(period)(MetersPerSecond(0))
      override val turnVelocity: Stream[AngularVelocity] = Stream.periodic(period)(DegreesPerSecond(0))
      var askedForInitPosition = false

      override val forwardPosition: Stream[Length] = Stream.periodic(period)(Feet(0))
      var checked = false

      // turn position in follow way points is relativised, so this is a work around
      // to simulate the robot later being at 45 degrees from the initial angle of 0
      override val turnPosition: Stream[Angle] = Stream.periodic(period){
        if (!checked) {
          checked = true
          Degrees(0)
        } else {
          Degrees(45)
        }
      }
    }

    val target = new Point(Feet(-1), Feet(-1))
    val task = new drive.unicycleTasks.FollowWayPoints(Seq(Point.origin, target), Feet(1), Percent(70))(drivetrainComp)

    task.init()
    ticker(Milliseconds(5))

    implicit val tolerance = Percent(0.01)
    val turn = lastAppliedSignal.turn


    assert(turn ~= Percent(0), s"Turn was actually $turn")
  }

  test("test that if near target, do not turn 90 degrees") {
    var lastAppliedSignal = zeroSignal
    val testDrivetrainComp = new TestDrivetrainComponent(lastAppliedSignal = _)

    val target = new Point(Feet(0), Feet(1))

    implicit val hardware = new UnicycleHardware {
      override val forwardVelocity: Stream[Velocity] = null

      override val turnVelocity: Stream[AngularVelocity] = Stream.periodic(period)(DegreesPerSecond(0))

      override val forwardPosition: Stream[Length] = Stream.periodic(period)(Feet(0))
      override val turnPosition: Stream[Angle] = Stream.periodic(period)(Degrees(0))
    }

    val task = new drive.unicycleTasks.FollowWayPointsWithPosition(Seq(Point.origin, target), Feet(0.1), hardware.forwardPosition.mapToConstant(Point(Feet(0.001), Feet(0.999999))), hardware.turnPosition.mapToConstant(Degrees(0)), Percent(70))(testDrivetrainComp)

    task.init()

    ticker(Milliseconds(5))

    assert(lastAppliedSignal.turn.abs <= Percent(1), s"Turn was ${lastAppliedSignal.turn.toPercent} %")
  }
}

