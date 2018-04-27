package com.lynbrookrobotics.potassium.commons.arm

import com.lynbrookrobotics.potassium.control.PIDConfig
import com.lynbrookrobotics.potassium.{ClockMocking, Component, Signal}
import com.lynbrookrobotics.potassium.streams.{Periodic, Stream}
import com.lynbrookrobotics.potassium.units.GenericValue
import org.scalatest.FunSuite
import squants.{Dimensionless, Percent}
import squants.motion.AngularVelocity
import squants.space.{Angle, Degrees}
import squants.time.Milliseconds
import com.lynbrookrobotics.potassium.units._
import GenericValue._

class ArmTaskTest extends FunSuite {
  val tickPeriod = Milliseconds(5)
  implicit val (clock, ticker) = ClockMocking.mockedClockTicker

  test("Arm starts below target and then moves up. After moving up, runs inner function") {
    val arm = new Arm {
      override type Properties = ArmProperties
      override type Hardware = ArmHardware
      override type Comp = Component[Dimensionless]
    }

    implicit val props = Signal.constant(new ArmProperties {
      override def positionGains
        : PIDConfig[Angle, Angle, GenericValue[Angle], AngularVelocity, GenericIntegral[Angle], Dimensionless] =
        PIDConfig[Angle, Angle, GenericValue[Angle], AngularVelocity, GenericIntegral[Angle], Dimensionless](
          Percent(100) / Degrees(90),
          Percent(0) / (Degrees(5) * Milliseconds(5)),
          Percent(0) / (Degrees(5) / Milliseconds(3))
        )
    })

    var currentAngle: Angle = Degrees(0)

    var armSignal: Dimensionless = null

    val testArm = new Component[Dimensionless] {
      override def defaultController: Stream[Dimensionless] = Stream.periodic(tickPeriod)(Percent(0))

      override def applySignal(signal: Dimensionless): Unit = {
        armSignal = signal
      }
    }

    implicit val hardware: ArmHardware = new ArmHardware {
      override val angle: Stream[Angle] = Stream.periodic(tickPeriod)(currentAngle)
    }

    val task = new arm.positionTasks.WhileAbovePosition(hardware.angle.mapToConstant(Degrees(90)))(testArm).toFinite

    task.init()

    ticker(Milliseconds(5))
    ticker(Milliseconds(5))
    ticker(Milliseconds(5))

    assert(armSignal == Percent(100))

    currentAngle = Degrees(45)
    ticker(Milliseconds(5))
    assert(armSignal == Percent(100))

    currentAngle = Degrees(95)
    ticker(Milliseconds(5))
    assert(!task.isRunning)
  }

  test("Arm starts above target and then moves down. After moving down, runs inner function") {
    val arm = new Arm {
      override type Properties = ArmProperties
      override type Hardware = ArmHardware
      override type Comp = Component[Dimensionless]
    }

    implicit val props = Signal.constant(new ArmProperties {
      override def positionGains
        : PIDConfig[Angle, Angle, GenericValue[Angle], AngularVelocity, GenericIntegral[Angle], Dimensionless] =
        PIDConfig[Angle, Angle, GenericValue[Angle], AngularVelocity, GenericIntegral[Angle], Dimensionless](
          Percent(100) / Degrees(90),
          Percent(0) / (Degrees(5) * Milliseconds(5)),
          Percent(0) / (Degrees(5) / Milliseconds(3))
        )
    })

    var currentAngle: Angle = Degrees(0)

    var armSignal: Dimensionless = null

    val testArm = new Component[Dimensionless] {
      override def defaultController: Stream[Dimensionless] = Stream.periodic(tickPeriod)(Percent(0))

      override def applySignal(signal: Dimensionless): Unit = {
        armSignal = signal
      }
    }

    implicit val hardware: ArmHardware = new ArmHardware {
      override val angle: Stream[Angle] = Stream.periodic(tickPeriod)(currentAngle)
    }

    val task = new arm.positionTasks.WhileBelowPosition(hardware.angle.mapToConstant(Degrees(90)))(testArm).toFinite

    task.init()

    currentAngle = Degrees(180)

    ticker(Milliseconds(5))
    ticker(Milliseconds(5))
    ticker(Milliseconds(5))

    assert(armSignal == Percent(-100))

    currentAngle = Degrees(135)
    ticker(Milliseconds(5))
    assert(armSignal == Percent(-100))

    currentAngle = Degrees(0)
    ticker(Milliseconds(5))
    assert(!task.isRunning)
  }

  test("Testing correct percentage outputs for given values when running proportional control") {
    val arm = new Arm {
      override type Properties = ArmProperties
      override type Hardware = ArmHardware
      override type Comp = Component[Dimensionless]
    }

    implicit val props = Signal.constant(new ArmProperties {
      override def positionGains
        : PIDConfig[Angle, Angle, GenericValue[Angle], AngularVelocity, GenericIntegral[Angle], Dimensionless] =
        PIDConfig[Angle, Angle, GenericValue[Angle], AngularVelocity, GenericIntegral[Angle], Dimensionless](
          Percent(100) / Degrees(90),
          Percent(0) / (Degrees(5) * Milliseconds(5)),
          Percent(0) / (Degrees(5) / Milliseconds(3))
        )
    })

    var currentAngle: Angle = Degrees(0)

    var armSignal: Dimensionless = null

    val testArm = new Component[Dimensionless] {
      override def defaultController: Stream[Dimensionless] = Stream.periodic(tickPeriod)(Percent(0))

      override def applySignal(signal: Dimensionless): Unit = {
        armSignal = signal
      }
    }

    implicit val hardware: ArmHardware = new ArmHardware {
      override val angle: Stream[Angle] = Stream.periodic(tickPeriod)(currentAngle)
    }

    val task =
      new arm.positionTasks.WhileAtPosition(hardware.angle.mapToConstant(Degrees(90)), Degrees(5))(testArm).toFinite

    task.init()

    ticker(Milliseconds(5))
    ticker(Milliseconds(5))
    ticker(Milliseconds(5))

    assert(armSignal == Percent(100))

    currentAngle = Degrees(45)
    ticker(Milliseconds(5))
    assert(armSignal == Percent(50))

    currentAngle = Degrees(180)
    ticker(Milliseconds(5))
    assert(armSignal == Percent(-100))

    currentAngle = Degrees(135)
    ticker(Milliseconds(5))
    assert(armSignal == Percent(-50))

    currentAngle = Degrees(90)
    ticker(Milliseconds(5))
    assert(!task.isRunning)
  }
}
