package com.lynbrookrobotics.potassium.commons.drivetrain

import com.lynbrookrobotics.potassium.control.PIDFConfig
import com.lynbrookrobotics.potassium.units.GenericValue._
import com.lynbrookrobotics.potassium.units._
import com.lynbrookrobotics.potassium.{PeriodicSignal, Signal, SignalLike}

import org.scalacheck.Prop.forAll

import org.scalatest.FunSuite
import org.scalatest.prop.Checkers._

import squants.motion.{AngularVelocity, DegreesPerSecond, MetersPerSecond, MetersPerSecondSquared}
import squants.space.Meters
import squants.time.{Milliseconds, Seconds}
import squants.{Each, Percent, Velocity}

class UnicycleDriveTest extends FunSuite {
  implicit val hardware: UnicycleHardware = new UnicycleHardware {
    override val forwardVelocity: Signal[Velocity] = Signal(MetersPerSecond(0))
    override val turnVelocity: Signal[AngularVelocity] = Signal(DegreesPerSecond(0))
  }

  private class TestDrivetrain extends UnicycleDrive {
    override type DriveSignal = UnicycleSignal
    override type DriveVelocity = UnicycleSignal

    override type DrivetrainHardware = UnicycleHardware
    override type DrivetrainProperties = UnicycleProperties

    override protected def convertUnicycleToDrive(uni: UnicycleSignal): DriveSignal = uni

    override protected val controlMode: UnicycleControlMode = NoOperation

    override protected def driveClosedLoop(signal: SignalLike[DriveSignal]): PeriodicSignal[DriveSignal] = signal.toPeriodic

    override type Drivetrain = Nothing
  }

  test("Open forward loop produces same forward speed as input and zero turn speed") {
    val drive = new TestDrivetrain

    check(forAll { x: Double =>
      val out = drive.UnicycleControllers.
        openForwardClosedDrive(Signal.constant(Each(x))).
        currentValue(Milliseconds(5))
      out.forward.toEach == x && out.turn.toEach == 0
    })
  }

  test("Open turn loop produces same turn speed as input and zero forward speed") {
    val drive = new TestDrivetrain

    check(forAll { x: Double =>
      val out = drive.UnicycleControllers.
        openTurnClosedDrive(Signal.constant(Each(x))).
        currentValue(Milliseconds(5))
      out.turn.toEach == x && out.forward.toEach == 0
    })
  }

  test("Closed loop with only feed-forward is essentially open loop") {
    implicit val props = new UnicycleProperties {
      override val maxForwardVelocity: Velocity = MetersPerSecond(10)
      override val maxTurnVelocity: AngularVelocity = DegreesPerSecond(10)

      override val forwardControlGains = PIDFConfig(
        Percent(0) / MetersPerSecond(1),
        Percent(0) / MetersPerSecondSquared(1),
        Percent(0) / Meters(1),
        Percent(100) / maxForwardVelocity
      )

      override val turnControlGains = PIDFConfig(
        Percent(0) / DegreesPerSecond(1),
        Percent(0) / (DegreesPerSecond(1).toGeneric / Seconds(1)),
        Percent(0) / (DegreesPerSecond(1).toGeneric * Seconds(1)),
        Percent(100) / maxTurnVelocity
      )
    }

    val drive = new TestDrivetrain

    check(forAll { (fwd: Double, turn: Double) =>
      val in = Signal.constant(UnicycleVelocity(MetersPerSecond(fwd), DegreesPerSecond(turn)))
      val out = drive.UnicycleControllers.
        velocityControl(in).
        currentValue(Milliseconds(5))

      (math.abs(out.forward.toEach - (fwd / 10)) / out.forward.toEach <= 0.0000001) &&
        (math.abs(out.turn.toEach - (turn / 10)) / out.turn.toEach <= 0.0000001)
    })
  }
}
