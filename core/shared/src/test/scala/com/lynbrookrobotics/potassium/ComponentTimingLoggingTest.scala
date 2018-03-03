package com.lynbrookrobotics.potassium

import com.lynbrookrobotics.potassium.streams._
import com.lynbrookrobotics.potassium.clock.Clock
import com.lynbrookrobotics.potassium.logging.AsyncLogger
import org.scalatest.FunSuite
import squants.Seconds

class ComponentTimingLoggingTest extends FunSuite {
  class MockedAsyncLogger(override val clock: Clock) extends AsyncLogger {
    var receivedLog = false
    override def info(msg: String): Unit = {
      receivedLog = true
    }

    def clearReceivedLog(): Unit = {
      receivedLog = false
    }
  }
  
  class MockedTimingLoggingComponent(clock: Clock) extends Component[Unit] with TimingLoggingComponent[Unit] {
    override val logger: MockedAsyncLogger = new MockedAsyncLogger(clock)

    override def defaultController: Stream[Unit] = ???
    
    override def applySignal(signal: Unit): Unit = {}
  }
  
  test("Logging component whose controller updates at or approximately at expected periodicity does not log an alarm") {
    val (clock, triggerClock) = ClockMocking.mockedClockTicker
    val period = Seconds(1)

    val component = new MockedTimingLoggingComponent(clock)
    component.setController(Stream.periodic(period)()(clock))

    // There has been no update and therefore should be no alarm
    assert(!component.logger.receivedLog)

    triggerClock(period)
    assert(!component.logger.receivedLog)

    triggerClock(period)
    assert(!component.logger.receivedLog)

    triggerClock(1.5 * period)
    assert(!component.logger.receivedLog)

    triggerClock(0.5 * period)
    assert(!component.logger.receivedLog)

    triggerClock(0.5 * period)
    assert(!component.logger.receivedLog)
  }

  test("Logging component whose controller updates slower than twice expected periodicity does logs an alarm") {
    val (clock, triggerClock) = ClockMocking.mockedClockTicker
    val period = Seconds(1)

    val component = new MockedTimingLoggingComponent(clock)
    val controller = Stream.periodic(period)()(clock)
    component.setController(controller)

    triggerClock(period)
    triggerClock(2 * period)
    assert(component.logger.receivedLog)
    component.logger.clearReceivedLog()
  }
}
