import com.lynbrookrobotics.potassium.Signal
import com.lynbrookrobotics.potassium.control.PIDF
import org.scalatest.FunSuite
import com.lynbrookrobotics.potassium.streams.{ExpectedPeriodicity, Periodic, Stream}
import com.lynbrookrobotics.potassium.units.Ratio
import squants.{Dimensionless, Percent, Time}
import squants.space.{Feet, Length}

class PIDFTest extends FunSuite{
  val period: Time = ???

  test("test P with error 1 foot and gain 50%/foot = 50%"){
    val (target, pubTarget) = Stream.manual[Length](Periodic(period))
    val (current, pubCurr) = Stream.manual[Length](Periodic(period))

    val gain = Signal(Ratio(Percent(50), Feet(1)))

    val pControl = PIDF.proportionalControl(current, target, gain)

    var lastOutput: Dimensionless = Percent(???)
    pControl.foreach(lastOutput = _)

    implicit val tolerance = Percent(0.1)
    assert(lastOutput ~= Percent(50))
  }

  test("test I control with error 1 foot and gain 50%/(foot/sec) for 1 sec = 50% and 2 = 100%"){
    val (target, pubTarget) = Stream.manual[Length](Periodic(period))
    val (current, pubCurr) = Stream.manual[Length](Periodic(period))

    val gain = Signal(Ratio(Percent(50), Feet(1)))

    val pControl = PIDF.integralControl(current, target, gain)

    var lastOutput: Dimensionless = _
    pControl.foreach(lastOutput = _)

    implicit val tolerance = Percent(0.1)
    assert(lastOutput ~= Percent(50))
  }

  test("test D control with error 1 ft and gain 50/(foot/") {
    ???
  }
}
