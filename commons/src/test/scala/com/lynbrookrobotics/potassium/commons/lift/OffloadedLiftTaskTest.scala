package com.lynbrookrobotics.potassium.commons.lift

import com.lynbrookrobotics.potassium.commons.lift.offloaded.{OffloadedLift, OffloadedLiftProperties}
import com.lynbrookrobotics.potassium.control.PIDConfig
import com.lynbrookrobotics.potassium.control.offload.OffloadedSignal.{OpenLoop, PositionBangBang}
import com.lynbrookrobotics.potassium.control.offload.{EscConfig, OffloadedSignal}
import com.lynbrookrobotics.potassium.streams.{Periodic, Stream}
import com.lynbrookrobotics.potassium.tasks.FiniteTask
import com.lynbrookrobotics.potassium.units.GenericValue._
import com.lynbrookrobotics.potassium.units._
import com.lynbrookrobotics.potassium.{ClockMocking, Component, Signal}
import org.scalatest.FunSuite
import squants.motion.Velocity
import squants.space.{Inches, Length}
import squants.time.Milliseconds
import squants.{Dimensionless, Each, Percent}

class OffloadedLiftTaskTest extends FunSuite {
  val tickPeriod = Milliseconds(5)
  val periodicity = Periodic(tickPeriod)
  implicit val (clock, ticker) = ClockMocking.mockedClockTicker

  test("OffloadedLift starts below target and then moves up. After moving up, runs inner function") {
    val lift = new OffloadedLift {
      override type Properties = OffloadedLiftProperties
      override type Hardware = LiftHardware
      override type Comp = Component[LiftSignal]

      override def openLoopToLiftSignal(x: Dimensionless): LiftSignal = null // not supposed to be called
    }

    implicit val props = Signal.constant(new OffloadedLiftProperties {
      override def positionGains: PIDConfig[Length, Length, GenericValue[Length], Velocity, GenericIntegral[Length], Dimensionless] =
        PIDConfig[Length, Length, GenericValue[Length], Velocity, GenericIntegral[Length], Dimensionless](
          Percent(100) / Inches(100),
          Percent(0) / (Inches(5) * Milliseconds(5)),
          Percent(0) / (Inches(5) / Milliseconds(3))
        )

      override val escConfig: EscConfig[Length] = EscConfig(ticksPerUnit = Ratio(Each(100), Inches(1)))

      override def toNative(height: Length): Dimensionless = escConfig.ticksPerUnit * height

      override def fromNative(native: Dimensionless): Length = escConfig.ticksPerUnit.recip * native
    })

    var currentLength: Length = Inches(0)

    var liftSignal: Dimensionless = null

    val testLift = new Component[OffloadedSignal] {
      override def defaultController: Stream[OffloadedSignal] = Stream.periodic(tickPeriod)(OpenLoop(Percent(0)))

      override def applySignal(signal: OffloadedSignal): Unit = {
        liftSignal = signal.asInstanceOf[PositionBangBang].signal
      }
    }

    implicit val hardware: LiftHardware = new LiftHardware {
      override def position: Stream[Length] = Stream.periodic(tickPeriod)(currentLength)
    }

    var innerCalled = false
    object StateTask extends FiniteTask() {
      override protected def onStart(): Unit = {
        innerCalled = true
        finished()
      }

      override protected def onEnd() = Unit
    }

    val task = new lift.positionTasks.WhileAbovePosition(
      hardware.position.mapToConstant(Inches(90))
    )(testLift)
      .apply(StateTask)

    currentLength = Inches(0)

    task.init()

    ticker(Milliseconds(5))
    assert(liftSignal == Each(9000) /* 90 inches & 100 ticks/inch*/)
    currentLength = Inches(45)
    ticker(Milliseconds(5))
    assert(!innerCalled)

    ticker(Milliseconds(5))
    assert(liftSignal == Each(9000) /* 90 inches & 100 ticks/inch*/)
    currentLength = Inches(89)
    ticker(Milliseconds(5))
    assert(!innerCalled)

    ticker(Milliseconds(5))
    assert(liftSignal == Each(9000) /* 90 inches & 100 ticks/inch*/)
    currentLength = Inches(91)
    ticker(Milliseconds(5))
    assert(innerCalled)
  }

  test("Lift starts above target and then moves down. After moving down, runs inner function") {
    val lift = new OffloadedLift {
      override type Properties = OffloadedLiftProperties
      override type Hardware = LiftHardware
      override type Comp = Component[LiftSignal]

      override def openLoopToLiftSignal(x: Dimensionless): LiftSignal = null // not supposed to be called
    }

    implicit val props = Signal.constant(new OffloadedLiftProperties {
      override def positionGains: PIDConfig[Length, Length, GenericValue[Length], Velocity, GenericIntegral[Length], Dimensionless] =
        PIDConfig[Length, Length, GenericValue[Length], Velocity, GenericIntegral[Length], Dimensionless](
          Percent(100) / Inches(100),
          Percent(0) / (Inches(5) * Milliseconds(5)),
          Percent(0) / (Inches(5) / Milliseconds(3))
        )

      override val escConfig: EscConfig[Length] = EscConfig(ticksPerUnit = Ratio(Each(100), Inches(1)))

      override def toNative(height: Length): Dimensionless = escConfig.ticksPerUnit * height

      override def fromNative(native: Dimensionless): Length = escConfig.ticksPerUnit.recip * native
    })

    var currentLength: Length = Inches(0)

    var liftSignal: Dimensionless = null

    val testLift = new Component[OffloadedSignal] {
      override def defaultController: Stream[OffloadedSignal] = Stream.periodic(tickPeriod)(OpenLoop(Percent(0)))

      override def applySignal(signal: OffloadedSignal): Unit = {
        liftSignal = signal.asInstanceOf[PositionBangBang].signal
      }
    }

    implicit val hardware: LiftHardware = new LiftHardware {
      override def position: Stream[Length] = Stream.periodic(tickPeriod)(currentLength)
    }

    var innerCalled = false
    object StateTask extends FiniteTask() {
      override protected def onStart(): Unit = {
        innerCalled = true
        finished()
      }

      override protected def onEnd() = Unit
    }

    val task = new lift.positionTasks.WhileBelowPosition(
      hardware.position.mapToConstant(Inches(90))
    )(testLift)
      .apply(StateTask)

    currentLength = Inches(180)

    task.init()

    ticker(Milliseconds(5))
    assert(liftSignal == Each(9000) /* 90 inches & 100 ticks/inch*/)
    currentLength = Inches(140)
    ticker(Milliseconds(5))
    assert(!innerCalled)

    ticker(Milliseconds(5))
    assert(liftSignal == Each(9000) /* 90 inches & 100 ticks/inch*/)
    currentLength = Inches(91)
    ticker(Milliseconds(5))
    assert(!innerCalled)

    ticker(Milliseconds(5))
    assert(liftSignal == Each(9000) /* 90 inches & 100 ticks/inch*/)
    currentLength = Inches(89)
    ticker(Milliseconds(5))
    assert(innerCalled)
  }
}
