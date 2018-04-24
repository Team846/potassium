package com.lynbrookrobotics.potassium.control

import com.lynbrookrobotics.potassium.streams.{Periodic, Stream}
import com.lynbrookrobotics.potassium.units.Ratio
import com.lynbrookrobotics.potassium.{ClockMocking, Signal}
import org.scalatest.FunSuite
import squants.motion.{FeetPerSecond, Velocity}
import squants.space.{Feet, Length}
import squants.time.Seconds
import squants.{Dimensionless, Percent, Time}

class PIDFTest extends FunSuite {
  val period: Time = Seconds(0.005)

  test("test P with error 1 foot and gain 50%/foot = 50%") {
    val (targetCurrent, pubTargetCurrent) = Stream.manual[(Length, Length)](Periodic(period, null))
    val target = targetCurrent.map(_._1)
    val current = targetCurrent.map(_._2)

    val gain = Signal(Ratio(Percent(50), Feet(1)))

    val pControl = PIDF.proportionalControl(current, target, gain)

    var lastOutput: Dimensionless = Percent(-10)
    pControl.foreach(lastOutput = _)

    pubTargetCurrent(Feet(1), Feet(0))

    implicit val tolerance = Percent(0.1)
    assert(lastOutput ~= Percent(50))
  }

  test("test I control with error 1 foot/s and gain 50%/((foot/sec)/s)") {
    implicit val (mockedClock, triggerClock) = ClockMocking.mockedClockTicker

    val (targetAndCurrent, pubTargetAndCurrent) = Stream.manualWithTime[(Velocity, Velocity)](period)
    val target = targetAndCurrent.map(_._1)
    val current = targetAndCurrent.map(_._2)

    val gain = Signal(Ratio(Percent(50), FeetPerSecond(1) * Seconds(1)))

    val iControl = PIDF.integralControl(current, target, gain)

    var lastOutput: Dimensionless = Percent(-10)
    iControl.foreach(lastOutput = _)

    pubTargetAndCurrent(FeetPerSecond(1), FeetPerSecond(0))
    pubTargetAndCurrent(FeetPerSecond(1), FeetPerSecond(0)) // twice so that dt can be calculated

    implicit val tolerance = Percent(0.1)

    assert(lastOutput ~= Percent(0))

    triggerClock.apply(Seconds(1))
    pubTargetAndCurrent(FeetPerSecond(1), FeetPerSecond(0))

    assert(lastOutput ~= Percent(50))

    triggerClock.apply(Seconds(1))
    pubTargetAndCurrent(FeetPerSecond(1), FeetPerSecond(0))

    assert(lastOutput ~= Percent(100))

  }

  test("test D control with derivative error 1 ft/s and gain 50%/(foot/s) gets -50%") {
    implicit val (mockedClock, triggerClock) = ClockMocking.mockedClockTicker

    val (targetCurrent, pubTargetCurrent) = Stream.manualWithTime[(Length, Length)](period)
    val target = targetCurrent.map(_._1)
    val current = targetCurrent.map(_._2)

    val gain = Signal(Ratio(Percent(50), FeetPerSecond(1)))

    val dControl = PIDF.derivativeControl(current, target, gain)

    var lastOutput: Dimensionless = Percent(-10)
    dControl.foreach(lastOutput = _)

    pubTargetCurrent(Feet(1), Feet(0))

    // error will change from 1 to 0, divided by 1 second, getting 1ft/s
    triggerClock.apply(Seconds(1))

    pubTargetCurrent((Feet(1), Feet(1)))

    implicit val tolerance = Percent(0.1)
    assert(lastOutput ~= Percent(-50))
  }
}
