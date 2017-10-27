package com.lynbrookrobotics.potassium.lighting

import com.lynbrookrobotics.potassium.{ClockMocking, Signal}
import com.lynbrookrobotics.potassium.streams.Stream
import com.lynbrookrobotics.potassium.tasks.Task
import org.scalatest.FunSuite
import squants.time.Milliseconds

class LightingTest extends FunSuite {
  val period = Milliseconds(20)

  test("Test static color"){
    val comm = new RXTXTwoWayComms {
      connected = true
      def dataToLog(): Unit ={
        logQueue.enqueue(data.toString)
      }
    }

    implicit val (mockedClock, trigger) = ClockMocking.mockedClockTicker
    val dummyComponent = new LightingComponent(1, comm, period)(mockedClock)
    val twoStream = Stream.periodic[Int](period)(2)
    val task = new DisplayLighting(twoStream, dummyComponent)

    dummyComponent.debug = true

    Task.executeTask(task)
    trigger(period)
    comm.dataToLog()
    assertResult("2")(comm.pullLog())

    Task.abortCurrentTask()
    trigger(period)
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
    implicit val (mockedClock, trigger) = ClockMocking.mockedClockTicker
    val dummyComponent = new LightingComponent(1, comm, period)(mockedClock)
    val twoStream = Stream.periodic(period)(2)
    val task = new DisplayLighting(twoStream, dummyComponent)

    dummyComponent.debug = true

    Task.executeTask(task)
    trigger(period)
    comm.dataToLog()
    assertResult("No data to show")(comm.pullLog())

    Task.abortCurrentTask()
    trigger(period)
    comm.dataToLog()
    assertResult("No data to show")(comm.pullLog())
  }
}
