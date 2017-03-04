package com.lynbrookrobotics.potassium.commons.drivetrain

import com.lynbrookrobotics.potassium.commons.cartesianPosition.XYPosition
import squants.space.{Degrees, Feet, Meters}
import squants.{Acceleration, Angle, Dimensionless, Each, Length, Percent, Quantity, Velocity}
import com.lynbrookrobotics.potassium.control.{PIDConfig, PIDF, PIDFConfig}
import com.lynbrookrobotics.potassium.testing.ClockMocking
import com.lynbrookrobotics.potassium.units.GenericValue._
import com.lynbrookrobotics.potassium.units._
import com.lynbrookrobotics.potassium.{Component, PeriodicSignal, Signal, SignalLike}
import com.lynbrookrobotics.potassium.units.{GenericDerivative, GenericIntegral, GenericValue}
import squants.motion.{AngularVelocity, DegreesPerSecond, MetersPerSecond, MetersPerSecondSquared}
import squants.motion._
import squants.space.{Degrees, Meters}
import squants.time.{Milliseconds, Seconds, TimeIntegral}
import org.scalatest.FunSuite

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
    UnicycleControllers.lowerLevelVelocityControl(signal).withCheck(_ => println("being used here"))
  }

  override type Drivetrain = Component[DriveSignal]
}

class PurePursuitTasksTest extends FunSuite {
  val zeroSignal = UnicycleSignal(Each(0), Each(0))
  val origin = new Point(Feet(0), Feet(0))

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

  class TestDrivetrainComp(onTick: (UnicycleSignal) => Unit) extends Component[UnicycleSignal](Milliseconds(5)) {

    override def defaultController: PeriodicSignal[UnicycleSignal] =
      Signal.constant(UnicycleSignal(Percent(0), Percent(0))).toPeriodic

    override def applySignal(signal: UnicycleSignal): Unit = {
      onTick(signal)
    }
  }

  test("If target is initally within tolerance, stop immediately") {
    var lastAppliedSignal = zeroSignal
    implicit val drivetrainComp = new TestDrivetrainComp(lastAppliedSignal = _)

    implicit val hardware = new UnicycleHardware {
      override val forwardVelocity: Signal[Velocity] = Signal(MetersPerSecond(0))
      override val turnVelocity: Signal[AngularVelocity] = Signal(DegreesPerSecond(0))

      override val forwardPosition: Signal[Length] = Signal.constant(Feet(0))
      override val turnPosition: Signal[Angle] = Signal.constant(Degrees(0))
    }
    val target = new Point(Feet(0), Feet(0.5))
    val task = new drive.unicycleTasks.GoToPoint(target, Feet(1))

    task.init()

    ticker(Milliseconds(5))
    assert(!task.isRunning, "The task didn't finish")
  }

  test("Test that having 1 way point directly ahead results in not turning") {
    var lastAppliedSignal = zeroSignal
    implicit val testDrivetrainComp = new TestDrivetrainComp(lastAppliedSignal = _)

    val target = new Point(Feet(0), Feet(1))
    val period = Milliseconds(5)
    val maxAccel = props.get.maxAcceleration

    implicit val hardware = new UnicycleHardware {
      override val forwardVelocity: Signal[Velocity] = null

      override val turnVelocity: Signal[AngularVelocity] = Signal(DegreesPerSecond(0))

      override val forwardPosition: Signal[Length] = Signal.constant(Feet(0))
      override val turnPosition: Signal[Angle] = Signal.constant(Degrees(0))
    }

    val task = new drive.unicycleTasks.GoToPoint(target, Feet(0.1))

    task.init()

    ticker(Milliseconds(5))
    println(s"last applied signal $lastAppliedSignal")
    assert(lastAppliedSignal.turn.toPercent == 0)
    implicit val tolerance = Each(0.01)
    assert(lastAppliedSignal.forward ~= Percent(40))
  }

  test("Test that going left and back 1 foot does not result in full turn"){
    var lastAppliedSignal = zeroSignal
    implicit val drivetrainComp = new TestDrivetrainComp(lastAppliedSignal = _)

    implicit val hardware = new UnicycleHardware {
      override val forwardVelocity: Signal[Velocity] = Signal(MetersPerSecond(0))
      override val turnVelocity: Signal[AngularVelocity] = Signal(DegreesPerSecond(0))
      var askedForInitPosition = false

      override val forwardPosition: Signal[Length] = Signal.constant(Feet(0))
      override val turnPosition: Signal[Angle] = Signal{
        if (!askedForInitPosition) {
          askedForInitPosition = true
          Degrees(0)
        } else {
          Degrees(45)
        }
      }
    }

    val target = new Point(Feet(-1), Feet(-1))
    val task = new drive.unicycleTasks.GoToPoint(target, Feet(1))

    task.init()

    ticker(Milliseconds(5))
    implicit val tolerance = Percent(0.01)
    val result = lastAppliedSignal.turn
    println(s"result $result")
    assert(result ~= Percent(0), "still turning")
  }


  def clamp(toClamp: Dimensionless): Dimensionless = {
    if (toClamp >= Each(1)) Each(1)
    else if (toClamp <= Each(-1)) Each(-1)
    else toClamp
  }

  test("simulate pure pursuit kinematics") {
//    class SimulationDrivetrain extends UnicycleDrive {
//      override type DriveSignal = UnicycleSignal
//      override type DriveVelocity = UnicycleSignal
//
//      override type Hardware = UnicycleHardware
//      override type Properties = UnicycleProperties
//
//      override protected def convertUnicycleToDrive(uni: UnicycleSignal): DriveSignal = uni
//
//      override protected def controlMode(implicit hardware: Hardware,
//        props: Properties): UnicycleControlMode = NoOperation
//
//      override protected def driveClosedLoop(signal: SignalLike[DriveSignal])
//        (implicit hardware: Hardware,
//          props: Signal[Properties]): PeriodicSignal[DriveSignal] = {
//        UnicycleControllers.lowerLevelVelocityControl(signal)
//      }
//
//      override type Drivetrain = Component[DriveSignal]
//    }

//    val drive = new TestDrivetrain
    println("************ starting simulation!*****************")
    var lastAppliedSignal = zeroSignal
    implicit val testDrivetrainComp = new TestDrivetrainComp(lastAppliedSignal = _)

    val target = new Point(Feet(10), Feet(1))
    val period = Milliseconds(5)
    val maxAccel = props.get.maxAcceleration


    val maxTurnAccel = new GenericDerivative[AngularVelocity](
      DegreesPerSecond(50).toDegreesPerSecond,
      DegreesPerSecond)

    implicit val hardware = new UnicycleHardware {
      def normalize(velocity: Velocity): Double = {
        if (velocity > props.get.maxForwardVelocity) {
          1D
        } else if (velocity < -props.get.maxForwardVelocity){
          -1D
        } else {
          velocity / props.get.maxForwardVelocity
        }
      }

      def normalize(velocity: AngularVelocity): Double = {
        if (velocity > props.get.maxTurnVelocity) {
          1D
        } else if (velocity < -props.get.maxTurnVelocity){
          -1D
        } else {
          velocity / props.get.maxTurnVelocity
        }
      }

      var lastVelocity = FeetPerSecond(0)
      var lastAngularVelocity = DegreesPerSecond(0)

      val directionMotion = Signal{
        if (lastVelocity.toFeetPerSecond >= 0) 1.0
        else -1.0
      }

      val dragAcceleration = directionMotion.map{ dir =>
        println(s"drag ${-dir * maxAccel * (normalize(lastVelocity.abs) + 0.1)}")
        -dir * maxAccel * (normalize(lastVelocity.abs) + 0.1)
      }

      val turnDrag = Signal(maxTurnAccel * (normalize(lastAngularVelocity) + 0.1))

      val appliedForwardAcceleration = Signal{
        maxAccel * clamp(lastAppliedSignal.forward)}
      val appliedTurnAcceleration = Signal(maxTurnAccel * clamp(lastAppliedSignal.turn))

      val netForwardAcceleration = appliedForwardAcceleration.zip(dragAcceleration).map{ a =>
        val (applied, drag) = a
        println(s"last applied signal $lastAppliedSignal")
        applied + drag
      }

      val netTurnAccleration = appliedTurnAcceleration.zip(turnDrag).map{ a =>
        a._1 - a._2
      }

      val periodicForwardVelocity = netForwardAcceleration.toPeriodic.integral[Velocity].withCheck(lastVelocity = _)
      override val forwardVelocity = periodicForwardVelocity.peek.map{
        _.getOrElse(FeetPerSecond(0))
      }

      val periodicTurnVelocity: PeriodicSignal[AngularVelocity] =
        netTurnAccleration.toPeriodic.map(_.timeIntegrated).map(identity(_))

      override val turnVelocity = periodicTurnVelocity.peek.map(_.getOrElse(DegreesPerSecond(0)))

      val periodicTurnPosition = periodicTurnVelocity.integral[Angle]
      override val turnPosition = periodicTurnPosition.peek.map(_.getOrElse(Degrees(0)))

      val periodicForwardPosition = periodicForwardVelocity.integral
      override val forwardPosition = periodicForwardPosition.peek.map{
        println("being used here")
        _.getOrElse(Feet(0))}

      var lastPosition: Point = origin
      val periodicPosition = XYPosition(
        turnPosition,
        forwardPosition
      ).withCheck(lastPosition = _)
    }

    val task = new drive.unicycleTasks.GoToPoint(target, Feet(0.1))/*(drive, props, hardware)*/

    task.init()

    for(_ <- 1 to 200) {
      hardware.appliedForwardAcceleration.get
      hardware.periodicForwardVelocity.currentValue(Milliseconds(5))
    }
//    assert(lastAppliedSignal.turn.toPercent != 0)
//    assert(lastAppliedSignal.forward.toPercent - 100 <= 0.01)
  }
}

