package com.lynbrookrobotics.potassium.commons.lift

import com.lynbrookrobotics.potassium.control.PIDConfig
import com.lynbrookrobotics.potassium.{ClockMocking, Component, Signal}
import com.lynbrookrobotics.potassium.streams.Periodic
import com.lynbrookrobotics.potassium.units._
import org.scalatest.FunSuite
import squants.{Dimensionless, Percent}
import squants.motion.Velocity
import squants.space.{Inches, Length}
import squants.time.Milliseconds
import com.lynbrookrobotics.potassium.streams.Stream

import GenericValue._

class LiftTaskTest extends FunSuite {
  val tickPeriod = Milliseconds(5)
  val periodicity = Periodic(tickPeriod)
  implicit val (clock, ticker) = ClockMocking.mockedClockTicker

  test("Lift starts below target and then moves up. After moving up, runs inner function") {
    val lift = new Lift {
      override type Properties = LiftProperties
      override type Hardware = LiftHardware
      override type Comp = Component[Dimensionless]
    }

    implicit val props = Signal.constant(new LiftProperties {
      override def positionGains: PIDConfig[Length, Length, GenericValue[Length], Velocity, GenericIntegral[Length], Dimensionless] =
        PIDConfig[Length, Length, GenericValue[Length], Velocity, GenericIntegral[Length], Dimensionless](
          Percent(100) / Inches(90),
          Percent(0) / (Inches(5) * Milliseconds(5)),
          Percent(0) / (Inches(5) / Milliseconds(3))
        )
    })

    var currentLength: Length = Inches(0)

    var liftSignal: Dimensionless = null

    val testLift = new Component[Dimensionless] {
      override def defaultController: Stream[Dimensionless] = Stream.periodic(tickPeriod)(Percent(0))

      override def applySignal(signal: Dimensionless): Unit = {
        liftSignal = signal
      }
    }

    implicit val hardware: LiftHardware = new LiftHardware {
      override def position: Stream[Length] = Stream.periodic(tickPeriod)(currentLength)
    }

    val task = new lift.positionTasks.WhileAbovePosition(hardware.position.mapToConstant(Inches(90)))(testLift).toFinite

    task.init()

    ticker(Milliseconds(5))
    ticker(Milliseconds(5))
    ticker(Milliseconds(5))

    assert(liftSignal == Percent(100))

    currentLength = Inches(45)
    ticker(Milliseconds(5))
    assert(liftSignal == Percent(100))

    currentLength = Inches(95)
    ticker(Milliseconds(5))
    assert(!task.isRunning)
  }

  test("Lift starts above target and then moves down. After moving down, runs inner function") {
    val lift = new Lift {
      override type Properties = LiftProperties
      override type Hardware = LiftHardware
      override type Comp = Component[Dimensionless]
    }

    implicit val props = Signal.constant(new LiftProperties {
      override def positionGains: PIDConfig[Length, Length, GenericValue[Length], Velocity, GenericIntegral[Length], Dimensionless] =
        PIDConfig[Length, Length, GenericValue[Length], Velocity, GenericIntegral[Length], Dimensionless](
          Percent(100) / Inches(90),
          Percent(0) / (Inches(5) * Milliseconds(5)),
          Percent(0) / (Inches(5) / Milliseconds(3))
        )
    })

    var currentLength: Length = Inches(0)

    var liftSignal: Dimensionless = null

    val testLift = new Component[Dimensionless] {
      override def defaultController: Stream[Dimensionless] = Stream.periodic(tickPeriod)(Percent(0))

      override def applySignal(signal: Dimensionless): Unit = {
        liftSignal = signal
      }
    }

    implicit val hardware: LiftHardware = new LiftHardware {
      override def position: Stream[Length] = Stream.periodic(tickPeriod)(currentLength)
    }

    val task = new lift.positionTasks.WhileBelowPosition(hardware.position.mapToConstant(Inches(90)))(testLift).toFinite

    task.init()

    currentLength = Inches(180)

    ticker(Milliseconds(5))
    ticker(Milliseconds(5))
    ticker(Milliseconds(5))

    assert(liftSignal == Percent(-100))

    currentLength = Inches(135)
    ticker(Milliseconds(5))
    assert(liftSignal == Percent(-100))

    currentLength = Inches(0)
    ticker(Milliseconds(5))
    assert(!task.isRunning)
  }

  test("Testing correct percentage outputs for given values when running proportional control") {
    val lift = new Lift {
      override type Properties = LiftProperties
      override type Hardware = LiftHardware
      override type Comp = Component[Dimensionless]
    }

    implicit val props = Signal.constant(new LiftProperties {
      override def positionGains: PIDConfig[Length, Length, GenericValue[Length], Velocity, GenericIntegral[Length], Dimensionless] =
        PIDConfig[Length, Length, GenericValue[Length], Velocity, GenericIntegral[Length], Dimensionless](
          Percent(100) / Inches(90),
          Percent(0) / (Inches(5) * Milliseconds(5)),
          Percent(0) / (Inches(5) / Milliseconds(3))
        )
    })

    var currentLength: Length = Inches(0)

    var liftSignal: Dimensionless = null

    val testLift = new Component[Dimensionless] {
      override def defaultController: Stream[Dimensionless] = Stream.periodic(tickPeriod)(Percent(0))

      override def applySignal(signal: Dimensionless): Unit = {
        liftSignal = signal
      }
    }

    implicit val hardware: LiftHardware = new LiftHardware {
      override def position: Stream[Length] = Stream.periodic(tickPeriod)(currentLength)
    }

    val task = new lift.positionTasks.WhileAtPosition(hardware.position.mapToConstant(Inches(90)), Inches(5))(testLift).toFinite

    task.init()

    ticker(Milliseconds(5))
    ticker(Milliseconds(5))
    ticker(Milliseconds(5))

    assert(liftSignal == Percent(100))

    currentLength = Inches(45)
    ticker(Milliseconds(5))
    assert(liftSignal == Percent(50))

    currentLength = Inches(180)
    ticker(Milliseconds(5))
    assert(liftSignal == Percent(-100))

    currentLength = Inches(135)
    ticker(Milliseconds(5))
    assert(liftSignal == Percent(-50))

    currentLength = Inches(90)
    ticker(Milliseconds(5))
    assert(!task.isRunning)
  }
}
