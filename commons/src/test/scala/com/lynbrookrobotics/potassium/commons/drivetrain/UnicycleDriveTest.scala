package com.lynbrookrobotics.potassium.commons.drivetrain

import com.lynbrookrobotics.potassium.control.PIDFConfig
import com.lynbrookrobotics.potassium.{PeriodicSignal, Signal, SignalLike}
import org.scalatest.FunSuite
import squants.{Each, Percent, Velocity}
import squants.motion.{AngularVelocity, DegreesPerSecond, MetersPerSecond, MetersPerSecondSquared}
import org.scalacheck.Prop.forAll
import org.scalatest.prop.Checkers._
import squants.time.{Milliseconds, Seconds}
import com.lynbrookrobotics.potassium.units._
import squants.space.Meters
import com.lynbrookrobotics.potassium.units.GenericValue._

class UnicycleDriveTest extends FunSuite {
  private trait TestDrivetrain extends UnicycleDrive {
    override type DriveSignal = UnicycleSignal
    override type DriveVelocity = UnicycleSignal

    override protected def convertUnicycleToDrive(uni: UnicycleSignal): DriveSignal = uni

    override protected val controlMode: UnicycleControlMode = NoOperation

    override protected def driveClosedLoop[C[_]](signal: SignalLike[DriveSignal, C]): PeriodicSignal[DriveSignal] = signal.toPeriodic

    override protected def driveVelocity[C[_]](velocity: SignalLike[DriveVelocity, C]): PeriodicSignal[DriveSignal] = velocity.toPeriodic

    override type Drivetrain = Nothing

    var currentForwardVelocity: Velocity = MetersPerSecond(0)
    var currentTurnVelocity: AngularVelocity = DegreesPerSecond(0)

    override protected val forwardVelocity: Signal[Velocity] = Signal(currentForwardVelocity)
    override protected val turnVelocity: Signal[AngularVelocity] = Signal(currentTurnVelocity)

    override protected val maxForwardVelocity: Velocity = MetersPerSecond(10)
    override protected val maxTurnVelocity: AngularVelocity = DegreesPerSecond(10)
  }

  test("Open forward loop produces same forward speed as input and zero turn speed") {
    val drive = new TestDrivetrain {
      override protected val forwardControlGains = null
      override protected val turnControlGains = null
    }

    check(forAll { x: Double =>
      val out = drive.UnicycleControllers.
        forwardOpen(Signal.constant(Each(x))).
        currentValue(Milliseconds(5))
      out.forward.toEach == x && out.turn.toEach == 0
    })
  }

  test("Open turn loop produces same turn speed as input and zero forward speed") {
    val drive = new TestDrivetrain {
      override protected val forwardControlGains = null
      override protected val turnControlGains = null
    }

    check(forAll { x: Double =>
      val out = drive.UnicycleControllers.
        turnOpen(Signal.constant(Each(x))).
        currentValue(Milliseconds(5))
      out.turn.toEach == x && out.forward.toEach == 0
    })
  }

  test("Closed loop with only feed-forward is essentially open loop") {
    val drive = new TestDrivetrain {
      override protected val forwardControlGains = PIDFConfig(
        Percent(0) / MetersPerSecond(1),
        Percent(0) / MetersPerSecondSquared(1),
        Percent(0) / Meters(1),
        Percent(100) / maxForwardVelocity
      )

      override protected val turnControlGains = PIDFConfig(
        Percent(0) / DegreesPerSecond(1),
        Percent(0) / (DegreesPerSecond(1).toGeneric / Seconds(1)),
        Percent(0) / (DegreesPerSecond(1).toGeneric * Seconds(1)),
        Percent(100) / maxTurnVelocity
      )
    }

    check(forAll { (fwd: Double, turn: Double) =>
      val in = Signal.constant(drive.UnicycleVelocity(MetersPerSecond(fwd), DegreesPerSecond(turn)))
      val out = drive.UnicycleControllers.
        velocity(in).
        currentValue(Milliseconds(5))

      (math.abs(out.forward.toEach - (fwd / 10)) / out.forward.toEach <= 0.0000001) &&
        (math.abs(out.turn.toEach - (turn / 10)) / out.turn.toEach <= 0.0000001)
    })
  }
}
