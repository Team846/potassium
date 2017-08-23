import com.lynbrookrobotics.potassium.{ClockMocking, Signal, units}
import com.lynbrookrobotics.potassium.control.PIDF
import com.lynbrookrobotics.potassium.streams.{Periodic, Stream}
import com.lynbrookrobotics.potassium.units.{GenericIntegral, Ratio}
import org.scalatest.FunSuite
import squants.space.{Feet, Length}
import squants.time.{Seconds, TimeIntegral}
import squants.{Dimensionless, Percent, Time}
import com.lynbrookrobotics.potassium.units.GenericValue._
import com.lynbrookrobotics.potassium.units._
import squants.motion.{FeetPerSecond, Velocity}

class PIDFTest extends FunSuite {
  val period: Time = Seconds(0.005)

  test("test P with error 1 foot and gain 50%/foot = 50%"){
    val (target, pubTarget) = Stream.manual[Length](Periodic(period))
    val (current, pubCurr) = Stream.manual[Length](Periodic(period))

    val gain = Signal(Ratio(Percent(50), Feet(1)))

    val pControl = PIDF.proportionalControl(current, target, gain)

    var lastOutput: Dimensionless = Percent(-10)
    pControl.foreach(lastOutput = _)

    pubCurr(Feet(0))
    pubTarget(Feet(1))

    implicit val tolerance = Percent(0.1)
    assert(lastOutput ~= Percent(50))
  }

  test("test I control with error 1 foot/s and gain 50%/((foot/sec)/s)"){
    val (mockedClock, triggerClock) = ClockMocking.mockedClockTicker

    val (target, pubTarget) = Stream.manual[Velocity](Periodic(period), mockedClock)
    val (current, pubCurr) = Stream.manual[Velocity](Periodic(period), mockedClock)

    val gain = Signal(
      Ratio(
        Percent(50),
        FeetPerSecond(1) * Seconds(1)))

    val iControl = PIDF.integralControl(current, target, gain)

    var lastOutput: Dimensionless = Percent(-10)
    iControl.foreach(lastOutput = _)

    pubTarget(FeetPerSecond(1))
    pubCurr(FeetPerSecond(0))

    implicit val tolerance = Percent(0.1)

    // TODO: fails
//    assert(lastOutput ~= Percent(0), s"output was: $lastOutput")

    triggerClock.apply(Seconds(1))
    pubTarget(FeetPerSecond(1))
    pubCurr(FeetPerSecond(0))

    assert(lastOutput ~= Percent(50))

    triggerClock.apply(Seconds(1))
    pubTarget(FeetPerSecond(1))
    pubCurr(FeetPerSecond(0))

    assert(lastOutput ~= Percent(100))

  }

  test("test D control with derivative error 1 ft/s and gain 50%/(foot/s) gets -50%") {
    val (mockedClock, triggerClock) = ClockMocking.mockedClockTicker

    val (target, pubTarget) = Stream.manual[Length](Periodic(period), mockedClock)
    val (current, pubCurr) = Stream.manual[Length](Periodic(period), mockedClock)

    val gain = Signal(Ratio(Percent(50), FeetPerSecond(1)))

    val dControl = PIDF.derivativeControl(current, target, gain)

    var lastOutput: Dimensionless = Percent(-10)
    dControl.foreach(lastOutput = _)

    pubTarget(Feet(1))
    pubCurr(Feet(0))

    // error will change from 1 to 0, divided by 1 second, getting 1ft/s
    triggerClock.apply(Seconds(1))

    pubTarget(Feet(1))
    pubCurr(Feet(1))

    implicit val tolerance = Percent(0.1)
    assert(lastOutput ~= Percent(-50))
  }
}
