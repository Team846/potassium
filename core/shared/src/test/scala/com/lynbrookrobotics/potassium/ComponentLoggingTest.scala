package com.lynbrookrobotics.potassium


import com.lynbrookrobotics.potassium.streams.Stream
import org.scalatest.FunSuite
import org.scalatest.time.Milliseconds
import squants.{Dimensionless, Percent}

class ComponentLoggingTest extends FunSuite{
  implicit val ticker = ClockMocking.mockedClockTicker
  val comp = new Component[Dimensionless] {
    override def defaultController: Stream[Dimensionless] = Stream.periodic(Milliseconds(5))(Percent(0))

    override def applySignal(signal: Dimensionless): Unit = {
      ticker(Milliseconds(10))
    }
  }

  val controller = Stream[Dimensionless]

  test ("Information is being logged") {
    var executed = false
    comp.logStuff(controller) { _ =>
      executed = true
    }
    assert(executed)
  }
}
