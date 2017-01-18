package com.lynbrookrobotics.potassium.lighting

import java.awt.Color

import com.lynbrookrobotics.potassium.testing.ClockMocking
import com.lynbrookrobotics.potassium.tasks.Task
import org.scalatest.FunSuite
import squants.time.Milliseconds

class LightingTest extends FunSuite {
  test("Test static color"){
    val comm = new TwoWayComm {
      isConnected = true
      def dataToLog(): Unit ={
        var tmp = ""
        while (dataQueue.nonEmpty){
          tmp += dataQueue.dequeue().toString
        }
        logQueue.enqueue(tmp)
      }
    }

    val (mockedClock, trigger) = ClockMocking.mockedClockTicker
    val dummyComponent = new LightingComponent(1, comm)(mockedClock)
    val signal = dummyComponent.staticColor(Color.WHITE)
    val task = new DisplayLighting(signal, dummyComponent)

    Task.executeTask(task)
    trigger(Milliseconds(20))
    comm.dataToLog()
    assertResult("000255255255")(comm.pullLog)

    Task.abortCurrentTask()
    trigger(Milliseconds(20))
    comm.dataToLog()
    assertResult("000000000000")(comm.pullLog)

    comm.dumpLog
    assertResult("No data to show")(comm.pullLog)
  }

  test("Test Failures"){
    val comm = new TwoWayComm {
      def dataToLog(): Unit = {
        if (dataQueue.nonEmpty) {
          var tmp = ""
          while (dataQueue.nonEmpty) {
            tmp += dataQueue.dequeue().toString
          }
          logQueue.enqueue(tmp)
        }
      }
    }
    val (mockedClock, trigger) = ClockMocking.mockedClockTicker
    val dummyComponent = new LightingComponent(1, comm)(mockedClock)
    val signal = dummyComponent.staticColor(Color.WHITE)
    val task = new DisplayLighting(signal, dummyComponent)

    Task.executeTask(task)
    trigger(Milliseconds(20))
    comm.dataToLog()
    assertResult("No data to show")(comm.pullLog)

    Task.abortCurrentTask()
    trigger(Milliseconds(20))
    comm.dataToLog()
    assertResult("No data to show")(comm.pullLog)
  }
}
