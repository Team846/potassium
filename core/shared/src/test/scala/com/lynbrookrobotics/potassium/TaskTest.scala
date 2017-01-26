package com.lynbrookrobotics.potassium

import com.lynbrookrobotics.potassium.tasks.{FiniteTask, Task}
import org.scalatest.{BeforeAndAfter, FunSuite}

class TaskTest extends FunSuite with BeforeAndAfter {
  after {
    Task.abortCurrentTask()
  }

  test("Single task execute and abort") {
    var started = false
    var aborted = false

    val task = new Task {
      override def init(): Unit = {
        started = true
      }

      override def abort(): Unit = {
        aborted = true
      }
    }

    assert(!started && !aborted)

    Task.executeTask(task)

    assert(started && !aborted)

    Task.abortCurrentTask()

    assert(started && aborted)
  }

  test("Single task execute, replace, then abort") {
    var firstStarted = false
    var firstAborted = false
    var secondStarted = false
    var secondAborted = false

    val first = new Task {
      override def init(): Unit = {
        firstStarted = true
      }

      override def abort(): Unit = {
        firstAborted = true
      }
    }

    val second = new Task {
      override def init(): Unit = {
        secondStarted = true
      }

      override def abort(): Unit = {
        secondAborted = true
      }
    }

    assert(!firstStarted && !firstAborted && !secondStarted && !secondAborted)

    Task.executeTask(first)

    assert(firstStarted && !firstAborted && !secondStarted && !secondAborted)

    Task.executeTask(second)

    assert(firstStarted && firstAborted && secondStarted && !secondAborted)

    Task.abortCurrentTask()

    assert(firstStarted && firstAborted && secondStarted && secondAborted)
  }

  test("FiniteTask.empty immediately finishes"){
    val task = FiniteTask.empty

    Task.executeTask(task)

    assert(!task.isRunning)
  }
}
