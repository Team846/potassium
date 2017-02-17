package com.lynbrookrobotics.potassium.lighting

import java.awt.Color

import com.lynbrookrobotics.potassium.Signal
import com.lynbrookrobotics.potassium.testing.ClockMocking
import com.lynbrookrobotics.potassium.tasks.Task
import org.scalatest.FunSuite
import squants.time.Milliseconds

class LightingTest extends FunSuite {
  test("Test static color"){
    val comm = new RXTXTwoWayComms {
      connected = true
      def dataToLog(): Unit ={
        logQueue.enqueue(data.toString)
      }
    }

    val (mockedClock, trigger) = ClockMocking.mockedClockTicker
    val dummyComponent = new LightingComponent(1, comm)(mockedClock)
    val task = new DisplayLighting(Signal[Int](2), dummyComponent)

    dummyComponent.debug = true

    Task.executeTask(task)
    trigger(Milliseconds(20))
    comm.dataToLog()
    assertResult("2")(comm.pullLog())

    Task.abortCurrentTask()
    trigger(Milliseconds(20))
    comm.dataToLog()
    assertResult("0")(comm.pullLog())

    comm.clearLog
    assertResult("No data to show")(comm.pullLog())
  }

  test("Test Failures"){
    val comm = new RXTXTwoWayComms {
      def dataToLog(): Unit ={
        logQueue.enqueue(data.toString)
      }
    }
    val (mockedClock, trigger) = ClockMocking.mockedClockTicker
    val dummyComponent = new LightingComponent(1, comm)(mockedClock)
    val task = new DisplayLighting(Signal[Int](2), dummyComponent)

    dummyComponent.debug = true

    Task.executeTask(task)
    trigger(Milliseconds(20))
    comm.dataToLog()
    assertResult("No data to show")(comm.pullLog())

    Task.abortCurrentTask()
    trigger(Milliseconds(20))
    comm.dataToLog()
    assertResult("No data to show")(comm.pullLog())
  }
}
