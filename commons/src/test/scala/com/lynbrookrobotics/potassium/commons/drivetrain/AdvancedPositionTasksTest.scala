//package com.lynbrookrobotics.potassium.commons.drivetrain
//
//import squants.space.{Degrees, Feet, Meters}
//import squants.{Acceleration, Angle, Dimensionless, Each, Length, Percent, Velocity}
//import com.lynbrookrobotics.potassium.control.{PIDConfig, PIDFConfig}
//import com.lynbrookrobotics.potassium.testing.ClockMocking
//import com.lynbrookrobotics.potassium.units.GenericValue._
//import com.lynbrookrobotics.potassium.units._
//import com.lynbrookrobotics.potassium.{Component, PeriodicSignal, Signal, SignalLike}
//import squants.motion.{AngularVelocity, DegreesPerSecond, MetersPerSecond, MetersPerSecondSquared}
//import squants.motion._
//import squants.space.{Degrees, Meters}
//import squants.time.{Milliseconds, Seconds}
//import org.scalatest.FunSuite
//
//class TestDrivetrain extends UnicycleDrive {
//  override type DriveSignal = UnicycleSignal
//  override type DriveVelocity = UnicycleSignal
//
//  override type Hardware = UnicycleHardware
//  override type Properties = UnicycleProperties
//
//  override protected def convertUnicycleToDrive(uni: UnicycleSignal): DriveSignal = uni
//
//  override protected def controlMode(implicit hardware: Hardware,
//                                     props: Properties): UnicycleControlMode = NoOperation
//
//  override protected def driveClosedLoop(signal: SignalLike[DriveSignal])
//                                        (implicit hardware: Hardware,
//                                         props: Signal[Properties]): PeriodicSignal[DriveSignal] = signal.toPeriodic
//
//  override type Drivetrain = Component[DriveSignal]
//}
//
//class AdvancedPositionTasksTest extends FunSuite {
//  val zeroSignal = UnicycleSignal(Each(0), Each(0))
//  val origin = new Point(Feet(0), Feet(0))
//
//  val props = Signal.constant(new UnicycleProperties {
//    override val maxForwardVelocity: Velocity = MetersPerSecond(10)
//    override val maxTurnVelocity: AngularVelocity = DegreesPerSecond(10)
//    override val maxAcceleration: Acceleration = FeetPerSecondSquared(10)
//    override val defaultLookAheadDistance: Length = Feet(0.5)
//
//    override val forwardControlGains = PIDConfig(
//      Percent(0) / MetersPerSecond(1),
//      Percent(0) / Meters(1),
//      Percent(0) / MetersPerSecondSquared(1)
//    )
//
//    override val turnControlGains = PIDConfig(
//      Percent(0) / DegreesPerSecond(1),
//      Percent(0) / Degrees(1),
//      Percent(0) / (DegreesPerSecond(1).toGeneric / Seconds(1))
//    )
//
//    override val forwardPositionControlGains = PIDConfig(
//      Percent(100) / Feet(1),
//      Percent(0) / (Meters(1).toGeneric * Seconds(1)),
//      Percent(0) / MetersPerSecond(1)
//    )
//
//    override val turnPositionControlGains = PIDConfig(
//      Percent(100) / Degrees(90),
//      Percent(0) / (Degrees(1).toGeneric * Seconds(1)),
//      Percent(0) / DegreesPerSecond(1)
//    )
//  })
//
//  implicit val (clock, ticker) = ClockMocking.mockedClockTicker
//  val drive = new TestDrivetrain
//
//  var lastAppliedSignal = zeroSignal
//  val drivetrain = new Component[UnicycleSignal](Milliseconds(5)) {
//    override def defaultController: PeriodicSignal[UnicycleSignal] =
//      Signal.constant(UnicycleSignal(Percent(0), Percent(0))).toPeriodic
//
//    override def applySignal(signal: UnicycleSignal): Unit = {
//      lastAppliedSignal = signal
//    }
//  }
//
//  //  implicit val hardware: UnicycleHardware = new UnicycleHardware {
//  //    override val forwardVelocity: Signal[Velocity] = Signal(MetersPerSecond(0))
//  //    override val turnVelocity: Signal[AngularVelocity] = Signal(DegreesPerSecond(0))
//  //    override val periodicPosition: PeriodicSignal[Point] = Signal.constant(origin).toPeriodic
//  //    override val position: Signal[Point] = Signal.constant(origin)
//  //
//  //
//  //    override val forwardPosition: Signal[Length] = Signal.constant(Feet(0))
//  //    override val turnPosition: Signal[Angle] = Signal.constant(Degrees(0))
//  //  }
//
//  test("If target is init position, stop immediately") {
//    implicit val hardware = new UnicycleHardware {
//      override val forwardVelocity: Signal[Velocity] = Signal(MetersPerSecond(0))
//      override val turnVelocity: Signal[AngularVelocity] = Signal(DegreesPerSecond(0))
//
//      override val forwardPosition: Signal[Length] = Signal.constant(Feet(0))
//      override val turnPosition: Signal[Angle] = Signal.constant(Degrees(0))
//    }
//
//    val task = new drive.unicycleTasks.GoToPoint(origin)(drivetrain, props, hardware)
//
//    task.init()
//
//    ticker(Milliseconds(5))
//    assert(!task.isRunning, "The task didn't finish")
//    assert(lastAppliedSignal.forward.toPercent == 0)
//  }
//
//  test("Test that having 1 way point directly ahead results in not turning") {
//    val target = new Point(Feet(0), Feet(1))
//    val period = Milliseconds(5)
//    lastAppliedSignal = zeroSignal
//    val maxAccel = props.get.maxAcceleration
//
//    implicit val hardware = new UnicycleHardware {
//      override val forwardVelocity: Signal[Velocity] = null
//
//      override val turnVelocity: Signal[AngularVelocity] = Signal(DegreesPerSecond(0))
//
//      override val forwardPosition: Signal[Length] = Signal.constant(Feet(0))
//      override val turnPosition: Signal[Angle] = Signal.constant(Degrees(0))
//    }
//
//    val task = new drive.unicycleTasks.GoToPoint(target)(drivetrain, props, hardware)
//
//    task.init()
//
//    ticker(Milliseconds(5))
//    assert(lastAppliedSignal.turn.toPercent == 0)
//    assert(lastAppliedSignal.forward.toPercent - 100 <= 0.01)
//  }
//
////  test("Test 2 way points, (1, 0) and (1, 1) after 1 second, position is approximately (1, 1)") {
////    val target = new Point(Feet(0), Feet(1))
////
////    lastAppliedSignal = zeroSignal
////    implicit val hardware = new UnicycleHardware {
////      override val forwardVelocity: Signal[Velocity] = Signal(MetersPerSecond(0))
////      override val turnVelocity: Signal[AngularVelocity] = Signal(DegreesPerSecond(0))
////      override val periodicPosition: PeriodicSignal[Point] = Signal.constant(origin).toPeriodic
////      override val position: Signal[Point] = Signal.constant(origin)
////
////
////      override val forwardPosition: Signal[Length] = Signal.constant(Feet(0))
////      override val turnPosition: Signal[Angle] = Signal.constant(Degrees(0))
////    }
////
////    // TODO: make current position be cacluated in the task itself, and add
////    // TODO: single waypoint task
////    val task = new drive.unicycleTasks.Follow2WayPoints(
////      List(hardware.position.get, target, target))(drivetrain, props, hardware)
////
////    task.init()
////
////    ticker(Milliseconds(5))
////    assert(lastAppliedSignal.turn.toPercent == 0)
////    assert(lastAppliedSignal.forward.toPercent - 100 <= 0.01)
////  }
//
//  test("simulate pure pursuit kinematics") {
//    val target = new Point(Feet(0), Feet(1))
//    val period = Milliseconds(5)
//    lastAppliedSignal = zeroSignal
//    val maxAccel = props.get.maxAcceleration
//    val maxTurnAccel = 50D // degrees /s^2 (no squants unit)
//
//    implicit val hardware = new UnicycleHardware {
//      var lastVelocity = FeetPerSecond(0)
//      override val forwardVelocity: Signal[Velocity] = Signal{
//        val drag = Each(lastVelocity / props.get.maxForwardVelocity + 0.1)
//        val normalizedForwardOutput = lastAppliedSignal.forward
//        val netNormalOutput = normalizedForwardOutput - drag
//
//        val newVelocity = lastVelocity + maxAccel * netNormalOutput * period
//        lastVelocity = newVelocity
//        newVelocity
//      }
//
//      var lastAngularVelocity = DegreesPerSecond(0)
//      override val turnVelocity: Signal[AngularVelocity] = Signal{
//        val forwardOutput = lastAppliedSignal.turn
//        val drag = Each(lastVelocity / props.get.maxTurnVelocity)
//        val netNormalizedOutput = forwardOutput - drag
//        val newAngularVelocity = lastAngularVelocity +
//          netNormalizedOutput.toEach * maxTurnAccel * period * Degrees(1)
//        lastAngularVelocity = newAngularVelocity
//        newAngularVelocity
//      }
//
////      override val periodicPosition: PeriodicSignal[Point] = /*xyPosition*/
////      override val position: Signal[Point] = Signal.constant(origin)
//
//
//      var lastdistanceTraveled = Feet(0)
//      override val forwardPosition: Signal[Length] = Signal{
//        val newDistanceTraveled = lastdistanceTraveled + lastVelocity * period
//        lastdistanceTraveled = newDistanceTraveled
//        newDistanceTraveled
//      }
//
//      var lastAngle = Degrees(0)
//      override val turnPosition: Signal[Angle] = Signal{
//        val newAngle = lastAngle + lastAngularVelocity * period
//        lastAngle = newAngle
//        newAngle
//      }
//    }
//
//    // TODO: make current position be cacluated in the task itself, and add
//    // TODO: single waypoint task
//    val task = new drive.unicycleTasks.GoToPoint(target)(drivetrain, props, hardware)
//
//    task.init()
//
//    ticker(Milliseconds(5))
//    assert(lastAppliedSignal.turn.toPercent == 0)
//    assert(lastAppliedSignal.forward.toPercent - 100 <= 0.01)
//  }
//}